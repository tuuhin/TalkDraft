package com.sam.talkdraft.transcription.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.model.ReadOnlyShortBuffer
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.transcription.domain.ITranscriberResultsProvider
import com.sam.talkdraft.transcription.domain.ITranscriptionEngine
import com.sam.talkdraft.transcription.domain.IVoiceDetectionProvider
import com.sam.talkdraft.transcription.domain.model.TranscriberConfig
import com.sam.talkdraft.transcription.domain.model.TranscriberEngine
import com.sam.talkdraft.transcription.domain.model.TranscriptionResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
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

    private fun resolveEngine(engineType: TranscriberEngine): ITranscriptionEngine = when (engineType) {
        TranscriberEngine.WHISPER -> whisperEngine
        TranscriberEngine.ZIP_FORMER -> zipFormerEngine
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    override fun transcribe(audioFrame: Flow<ReadOnlyShortBuffer>): Flow<TranscriptionResult> =
        _engineType.flatMapLatest { engineType ->
            // in case engine is not setup return idle state
            if (engineType == null) return@flatMapLatest flowOf(TranscriptionResult.Idle)
            // otherwise resolve the state and perform operation
            val engine = resolveEngine(engineType)
            audioFrame.runTranscriptionEngine(engine)
        }


    override suspend fun setConfig(config: TranscriberConfig, engine: TranscriberEngine): Result<Boolean> {
        return try {
            // setup the vad in all cases
            Logger.d(tag = TAG) { "PREPARING VAD" }
            val setupSuccess = vad.setup()
            // resolve engine and warm it up
            val requiredEngine = resolveEngine(engine) ?: return Result.failure(IllegalStateException("Invalid engine"))
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
        val engineType = _engineType.value ?: return
        val engine = resolveEngine(engineType)
        try {
            engine.reset()
            Logger.i(tag = TAG) { "ENGINE RESET COMPLETED" }
        } catch (e: Exception) {
            Logger.e(tag = TAG, throwable = e) { "FAILED TO RESET THE ENGINE" }
        } finally {
            Logger.i(tag = TAG) { "CLEANUP ON VAD" }
            vad.cleanup()
            Logger.i(tag = TAG) { "ENGINE CLEANUP ENGINE: ${engine::class.simpleName} SUCCESS" }
            engine.cleanUp()
        }
    }

    override suspend fun reset() {
        try {
            val engineType = _engineType.value ?: return
            val engine = resolveEngine(engineType)
            // flush out the segments and then reset
            vad.flushSegments()
            vad.reset()
            // reset the engine
            engine.reset()
            Logger.i(tag = TAG) { "VAD RESET AND ENGINE RESET COMPLETED" }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.e(tag = TAG, throwable = e) { "SOMETHING WHEN WRONG WITH RESET" }
        }
    }

    private fun Flow<ReadOnlyShortBuffer>.runTranscriptionEngine(engine: ITranscriptionEngine) = channelFlow {
        trySend(TranscriptionResult.Preparing)

        // one coroutine will load in the buffers
        launch {
            this@runTranscriptionEngine.collect { frame ->
                val frameArray = frame.toShortArray()
                // responsible to load in the buffers into the vad
                vad.processAudioBuffer(frameArray)
            }
        }

        // another coroutine to
        launch {
            vad.speechSegments.collect { frame ->
                val frameArray = frame.samples.toShortArray()
                val state = engine.processSegment(frameArray)
                send(state)
                if (state is TranscriptionResult.Success) {
                    val updatedSegment = state.segment.copy(
                        startTimeMs = frame.timedDuration.start,
                        endTime = frame.timedDuration.endInclusive,
                    )
                    send(state.copy(segment = updatedSegment))
                    // we need to reset the engine to get only the value of the current block
                    engine.reset()
                } else {
                    send(state)
                }
            }
        }
    }.flowOn(dispatchers.io)


    companion object {
        const val TAG = "TRANSCRIPTOR_RESULTS_PROVIDER"
    }
}

