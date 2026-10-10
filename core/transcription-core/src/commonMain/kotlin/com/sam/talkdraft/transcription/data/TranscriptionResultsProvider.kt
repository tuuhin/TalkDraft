package com.sam.talkdraft.transcription.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.model.ReadOnlyShortBuffer
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.transcription.domain.ITranscriberResultsProvider
import com.sam.talkdraft.transcription.domain.ITranscriptionEngine
import com.sam.talkdraft.transcription.domain.IVoiceDetectionProvider
import com.sam.talkdraft.transcription.domain.model.TimedVoiceDetectionSegment
import com.sam.talkdraft.transcription.domain.model.TranscriberConfig
import com.sam.talkdraft.transcription.domain.model.TranscriberEngine
import com.sam.talkdraft.transcription.domain.model.TranscriptionEngineOutput
import com.sam.talkdraft.transcription.domain.model.TranscriptionResult
import com.sam.talkdraft.transcription.domain.model.TranscriptionSegmentModel
import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
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

        // we will be buffering the frames and unlimited segments channel as segment generation is slower
        val frames = Channel<ShortArray>(Channel.BUFFERED)
        val segments = Channel<TimedVoiceDetectionSegment>(Channel.UNLIMITED)

        // read the values to the vad processor
        launch {
            try {
                this@runTranscriptionEngine.collect { frame ->
                    val arr = frame.toShortArray()
                    // mark the vad to process the frame and send the frame to the channel
                    vad.processAudioBuffer(arr)
                    // send to frame buffer channel
                    frames.send(arr)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.e(tag = TAG, throwable = e) { "FAILED TO COLLECT ENGINE AUDIO SEGMENTS" }
            }
        }

        // reading the speech segments from the vad itself
        launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                vad.speechSegments.collect(segments::send)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.e(tag = TAG, throwable = e) { "FAILED TO FEED THE SEGMENTS TO ENGINE" }
            }
        }

        // now the channel lopper
        launch(context = dispatchers.io) {
            try {
                while (isActive) {
                    select {
                        // priority to segments first
                        segments.onReceive { vadTimedSegment ->
                            // if a segment is found we will clear the whole frames channel
                            // clear all the audio frames as we are doing segments so these will not interfere
                            while (isActive) {
                                if (!frames.tryReceive().isSuccess) break
                            }
                            // reset the engine then process the segments
                            engine.reset()
                            // then process the current segment from scratch
                            val state = engine.processSegment(vadTimedSegment.samples.toShortArray())
                            send(state.toTranscriptionResult(vadTimedSegment.timedDuration))
                            // then again reset so the new data will get a new process segment block for new buffers
                            engine.reset()
                        }

                        // send plain buffers to the
                        frames.onReceive { audioBuffer ->
                            val state = engine.processSegment(audioBuffer)
                            send(state.toTranscriptionResult(null))
                        }
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.e(tag = TAG, throwable = e) { "ENGINE LOOP FAILED" }
            }
        }

        awaitClose {
            // close the channels
            frames.close()
            segments.close()
            Logger.d(tag = TAG) { "CLOSING IN TRANSCRIPTION FLOW" }
        }
    }.flowOn(dispatchers.io)
        .distinctUntilChanged()

    private fun resolveEngine(engineType: TranscriberEngine): ITranscriptionEngine = when (engineType) {
        TranscriberEngine.WHISPER -> whisperEngine
        TranscriberEngine.ZIP_FORMER -> zipFormerEngine
    }

    private fun TranscriptionEngineOutput.toTranscriptionResult(blockDuration: ClosedRange<Duration>? = null): TranscriptionResult {
        return when (this) {
            is TranscriptionEngineOutput.InvalidResult -> TranscriptionResult.Failed(error = error, message = message)
            is TranscriptionEngineOutput.Segment if text.isBlank() -> TranscriptionResult.Listening
            is TranscriptionEngineOutput.Segment -> {
                val segment = TranscriptionSegmentModel(segmentId = segmentId, text = text)
                TranscriptionResult.Success(
                    segment = segment,
                    isRealtime = blockDuration == null,
                    segmentDuration = blockDuration,
                )
            }
            TranscriptionEngineOutput.Buffering -> TranscriptionResult.Listening
        }
    }

    companion object {
        private const val TAG = "TRANSCRIPTOR_RESULTS_PROVIDER"
    }
}

