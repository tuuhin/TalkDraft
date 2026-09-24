package com.sam.talkdraft.transcription.data

import com.sam.talkdraft.common.model.ReadOnlyShortBuffer
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.transcription.domain.ITranscriberResultsProvider
import com.sam.talkdraft.transcription.domain.ITranscriptionEngine
import com.sam.talkdraft.transcription.domain.IVoiceDetectionProvider
import com.sam.talkdraft.transcription.domain.model.TranscriberConfig
import com.sam.talkdraft.transcription.domain.model.TranscriberEngine
import com.sam.talkdraft.transcription.domain.model.TranscriptionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

@Factory(binds = [ITranscriberResultsProvider::class])
internal class TranscriptionResultsProvider(
    private val vad: IVoiceDetectionProvider,
    private val vadProcessor: VoiceAudioAccumulator,
    private val dispatchers: IPlatformCoroutineDispatchers,
    @Named("whisper_engine")
    private val whisperEngine: ITranscriptionEngine,
    @Named("zip_former_engine")
    private val zipFormerEngine: ITranscriptionEngine,
) : ITranscriberResultsProvider {

    private val _config = MutableStateFlow<TranscriberConfig?>(null)
    private val _engine = MutableStateFlow<TranscriberEngine?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun transcribe(audioFrame: Flow<ReadOnlyShortBuffer>)
        : Flow<TranscriptionState> = combine(_config, _engine) { c, e -> c to e }
        .flatMapLatest { (config, engine) ->
            // if config is not set mark not running
            if (config == null || engine == null) return@flatMapLatest flowOf(TranscriptionState.NotRunning)

            // else perform transcription
            flow {
                emit(TranscriptionState.RunningOrProcessing)

                audioFrame.collect { frame ->
                    val frame = frame.toShortArray()
                    // process the audio buffer
                    val detection = vad.processAudioBuffer(frame)
                    // determine the speech segment
                    val speechSegment = vadProcessor.process(audioFrame = frame, detection = detection)
                    if (speechSegment != null) {
                        when (engine) {
                            TranscriberEngine.WHISPER -> emit(whisperEngine.process(speechSegment))
                            TranscriberEngine.ZIP_FORMER -> emit(zipFormerEngine.process(speechSegment))
                        }
                    }
                }
            }.flowOn(dispatchers.default)
                .onStart {
                    setupEngine(engine, config)
                    vad.setup()
                    vadProcessor.reset()
                }.onCompletion {
                    // check anything remains
                    // cleanup vad
                    vad.cleanup()
                    val segment = vadProcessor.flush() ?: return@onCompletion
                    processAndCleanUp(engine, segment)
                }
        }

    override fun setConfig(config: TranscriberConfig?, engine: TranscriberEngine) {
        // will reset the flow when config is null otherwise create a new flow based on the given condition
        _config.update { config }
        _engine.update { engine }
    }

    private suspend fun setupEngine(engine: TranscriberEngine, config: TranscriberConfig) {
        when (engine) {
            TranscriberEngine.WHISPER -> whisperEngine.warmUp(config)
            TranscriberEngine.ZIP_FORMER -> zipFormerEngine.warmUp(config)
        }
    }

    private fun processAndCleanUp(engine: TranscriberEngine, bytes: ShortArray) {
        val engine = when (engine) {
            TranscriberEngine.WHISPER -> whisperEngine
            TranscriberEngine.ZIP_FORMER -> zipFormerEngine
        }
        engine.process(bytes)
        engine.cleanUp()
    }
}

