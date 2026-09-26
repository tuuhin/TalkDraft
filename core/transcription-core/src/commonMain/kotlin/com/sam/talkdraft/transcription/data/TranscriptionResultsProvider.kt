package com.sam.talkdraft.transcription.data

import co.touchlab.kermit.Logger
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

    private fun resolveEngine(engineType: TranscriberEngine?): ITranscriptionEngine? = when (engineType) {
        TranscriberEngine.WHISPER -> whisperEngine
        TranscriberEngine.ZIP_FORMER -> zipFormerEngine
        else -> null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun transcribe(audioFrame: Flow<ReadOnlyShortBuffer>): Flow<TranscriptionState> =
        combine(_config, _engine) { config, engineType -> config to engineType }
            .flatMapLatest { (config, engineType) ->
                val engine = resolveEngine(engineType)
                if (config == null || engine == null || engineType == null) {
                    return@flatMapLatest flowOf(TranscriptionState.Idle)
                }

                audioFrame.runTranscriptionEngine(engine)
                    .onStart {
                        emit(TranscriptionState.Idle)
                        setupEngine(engine, engineType, config)
                        emit(TranscriptionState.Ready)
                    }
                    .onCompletion { processAndCleanUp(engine, engineType, vadProcessor.flush()) }
            }


    override fun setConfig(config: TranscriberConfig?, engine: TranscriberEngine) {
        // will reset the flow when config is null otherwise create a new flow based on the given condition
        Logger.d(tag = TAG) { "ENGINE CONFIG ENGINE:$engine CONFIG:$config" }
        _config.update { config }
        _engine.update { engine }
    }

    /**
     * Collect the buffers and process the stream
     */
    private fun Flow<ReadOnlyShortBuffer>.runTranscriptionEngine(engine: ITranscriptionEngine) = flow {
        emit(TranscriptionState.Preparing)
        collect { frame ->
            val frameArray = frame.toShortArray()
            val detection = vad.processAudioBuffer(frameArray)

            val speechSegment = vadProcessor.process(frameArray, detection)
            val state = engine.process(frameArray)
            emit(state)
            if (speechSegment != null) engine.reset()
        }
    }.flowOn(dispatchers.io)

    /**
     * Setups up the engine and vad
     */
    private suspend fun setupEngine(
        engine: ITranscriptionEngine,
        engineType: TranscriberEngine,
        config: TranscriberConfig,
    ) {
        Logger.d(tag = TAG) { "ENGINE SETUP ENGINE:$engineType CONFIG:$config" }
        engine.warmUp(config)
        Logger.d(tag = TAG) { "SETTING UP VAD" }
        vad.setup()
        vadProcessor.reset()
    }

    /**
     * Cleanup with engine and reset vad
     */
    private suspend fun processAndCleanUp(
        engine: ITranscriptionEngine,
        engineType: TranscriberEngine,
        bytes: ShortArray?,
    ) {
        try {
            if (bytes != null && bytes.isNotEmpty()) {
                engine.process(bytes)
                engine.reset()
            }
        } catch (e: Exception) {
            Logger.e(tag = TAG, throwable = e) { "Error processing remaining audio buffer on cleanup" }
        } finally {
            Logger.d(tag = TAG) { "CLEANING UP THE VAD" }
            vadProcessor.reset()
            vad.cleanup()
            Logger.d(tag = TAG) { "ENGINE CLEANUP ENGINE:$engineType" }
            engine.cleanUp()
        }
    }

    companion object {
        const val TAG = "TRANSCRIPTOR_RESULTS_PROVIDER"
    }
}

