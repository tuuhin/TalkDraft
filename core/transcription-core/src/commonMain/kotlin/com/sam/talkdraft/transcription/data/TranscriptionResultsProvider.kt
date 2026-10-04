package com.sam.talkdraft.transcription.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.model.ReadOnlyShortBuffer
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.transcription.domain.ITranscriberResultsProvider
import com.sam.talkdraft.transcription.domain.ITranscriptionEngine
import com.sam.talkdraft.transcription.domain.IVoiceDetectionProvider
import com.sam.talkdraft.transcription.domain.model.TranscriberConfig
import com.sam.talkdraft.transcription.domain.model.TranscriberEngine
import com.sam.talkdraft.transcription.domain.model.TranscriptionEngineOutput
import com.sam.talkdraft.transcription.domain.model.TranscriptionResult
import com.sam.talkdraft.transcription.domain.model.TranscriptionSegmentModel
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

@Factory(binds = [ITranscriberResultsProvider::class])
internal class TranscriptionResultsProvider(
    private val vad: IVoiceDetectionProvider,
    private val dispatchers: IPlatformCoroutineDispatchers,
    @Named("whisper_engine")
    private val whisperEngine: ITranscriptionEngine,
    @Named("zip_former_engine")
    private val zipFormerEngine: ITranscriptionEngine,
) : ITranscriberResultsProvider {

    private val _engineType = MutableStateFlow<TranscriberEngine?>(null)
    override val isTranscriberReady: Flow<Boolean> = _engineType.map { it != null }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun transcribe(audioFrame: Flow<ReadOnlyShortBuffer>): Flow<TranscriptionResult> =
        _engineType.flatMapLatest { engineType ->
            // in case engine is not setup return idle state
            // also when the engine is clear we get state idle
            if (engineType == null) {
                return@flatMapLatest flow {
                    emit(TranscriptionResult.Idle)
                    // will keep the flow alive without any value propagation
                    awaitCancellation()
                }
            }
            // otherwise resolve the state and perform operation
            val engine = resolveEngine(engineType)
            audioFrame.runTranscriptionEngine(engine)
        }


    override suspend fun setConfig(
        config: TranscriberConfig,
        engine: TranscriberEngine,
    ): Result<Boolean> {
        return try {
            // setup the vad in all cases
            Logger.d(tag = TAG) { "SETTING UP VAD" }
            val setupSuccess = vad.setup()
            // resolve engine and warm it up
            val requiredEngine = resolveEngine(engine)
            Logger.d(tag = TAG) { "WARMING UP ENGINE:$engine CONFIG:$config" }
            requiredEngine.warmUp(config)
            // update the engine
            _engineType.update { engine }
            // everything is oke
            Logger.i(tag = TAG) { "ENGINE SETUP COMPLETED" }
            Result.success(setupSuccess)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }


    override fun clearConfig() {
        val engineType = _engineType.value ?: run {
            Logger.w(tag = TAG) { "NO ENGINE TYPE IS BEING SELECTED" }
            return
        }
        val engine = resolveEngine(engineType)
        try {
            engine.reset()
            Logger.i(tag = TAG) { "ENGINE RESET COMPLETED" }
        } catch (e: Exception) {
            Logger.e(tag = TAG, throwable = e) { "FAILED TO RESET THE ENGINE" }
        } finally {
            // clear the engine first
            _engineType.value = null
            // then the vad and the engine itself
            Logger.i(tag = TAG) { "CLEANUP ON VAD" }
            vad.cleanup()
            Logger.i(tag = TAG) { "ENGINE CLEANUP ENGINE: ${engine::class.simpleName} SUCCESS" }
            engine.cleanUp()
        }
    }

    override suspend fun reset() {
        // Use reset when we just perform reset on the engine
        // not the full cleanup
        try {
            val engineType = _engineType.value ?: return
            val engine = resolveEngine(engineType)
            // flush out the segments and then reset
            vad.flushSegments()
            Logger.d(tag = TAG) { "SEGMENTS FLUSHED OUT" }
            vad.reset()
            Logger.d(tag = TAG) { "VAD RESET DONE" }
            // reset the engine
            engine.reset()
            Logger.i(tag = TAG) { "RESET COMPLETED" }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.e(tag = TAG, throwable = e) { "SOMETHING WHEN WRONG WITH RESET" }
        }
    }

    private fun Flow<ReadOnlyShortBuffer>.runTranscriptionEngine(engine: ITranscriptionEngine) = channelFlow {
        // ready
        trySend(TranscriptionResult.Ready)

        // one coroutine will load in the buffers
        launch {
            try {
                this@runTranscriptionEngine.collect { frame ->
                    val frameArray = frame.toShortArray()
                    // marking them as finalized means we don't want to save them
                    val state = engine.processSegment(frameArray)
                    val transcriptionState = state.toTranscriptionResult(null)
                    trySend(transcriptionState)
                    // responsible to load in the buffers into the vad
                    vad.processAudioBuffer(frameArray)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.e(tag = TAG, throwable = e) { "FAILED TO COLLECT ENGINE AUDIO SEGMENTS" }
            }
        }

        // another coroutine to send the segments to the engine for recognization
        launch {
            try {
                vad.speechSegments.collect { frame ->
                    // finalized segments with data
                    val frameArray = frame.samples.toShortArray()
                    val state = engine.processSegment(frameArray)
                    // in case it's a missed state or idc state then pass it though
                    val finalState = state.toTranscriptionResult(frame.timedDuration)
                    send(finalState)
                    // then again another reset to do new segment
                    engine.reset()
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.e(tag = TAG, throwable = e) { "FAILED TO DEED THE SEGMENTS TO ENGINE" }
            }
        }
    }
        .flowOn(dispatchers.io)

    private fun resolveEngine(engineType: TranscriberEngine): ITranscriptionEngine = when (engineType) {
        TranscriberEngine.WHISPER -> whisperEngine
        TranscriberEngine.ZIP_FORMER -> zipFormerEngine
    }

    private fun TranscriptionEngineOutput.toTranscriptionResult(timedDuration: ClosedRange<Duration>? = null): TranscriptionResult {
        return when (this) {
            is TranscriptionEngineOutput.InvalidResult -> TranscriptionResult.Failed(error = error, message = message)
            is TranscriptionEngineOutput.Segment if (timedDuration != null) -> TranscriptionResult.Success(
                segment = TranscriptionSegmentModel(segmentId = segmentId, text = text, timedDuration),
                isFinalBlock = true,
            )

            is TranscriptionEngineOutput.Segment -> TranscriptionResult.Success(
                segment = TranscriptionSegmentModel(segmentId = segmentId, text = text),
                isFinalBlock = false,
            )

            TranscriptionEngineOutput.Buffering -> TranscriptionResult.Ready
        }
    }

    companion object {
        const val TAG = "TRANSCRIPTOR_RESULTS_PROVIDER"
    }
}

