package com.sam.talkdraft.feature_recorder

import com.sam.talkdraft.common.model.ReadOnlyFloatBuffer
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.common.platform.IPlatformFilePathProvider
import com.sam.talkdraft.model_manager.domain.repository.ISelectedTranscriptionModelStore
import com.sam.talkdraft.recorder.domain.IVoiceRecorderWithByteReader
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.recorder_visualizer.domain.IAudioDynamicVisualizer
import com.sam.talkdraft.transcription.domain.ITranscriberResultsProvider
import com.sam.talkdraft.transcription.domain.model.TranscriberConfig
import com.sam.talkdraft.transcription.domain.model.TranscriberEngine
import com.sam.talkdraft.transcription.domain.model.TranscriptionState
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

@Factory(binds = [ISimpleVoiceRecorder::class])
class SimpleVoiceRecorderImpl(
    private val dispatchers: IPlatformCoroutineDispatchers,
    private val transcriber: ITranscriberResultsProvider,
    private val selectedModelProvider: ISelectedTranscriptionModelStore,
    private val filesProvider: IPlatformFilePathProvider,
) : ISimpleVoiceRecorder, KoinComponent {

    private val _scope = CoroutineScope(dispatchers.default + SupervisorJob())

    private val _recorder: IVoiceRecorderWithByteReader by inject(parameters = { parametersOf(_scope) })
    private val _ampsReader: IAudioDynamicVisualizer by inject(parameters = { parametersOf(_recorder) })

    override val recorderState: Flow<RecorderState> = _recorder.state
    override val elapsedTime: Flow<Duration> = _recorder.elapsedTime

    override val waveform: Flow<ReadOnlyFloatBuffer?>
        get() = _ampsReader.waveformFlow(30)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val transcription: Flow<TranscriptionState>
        get() = transcriber.transcribe(_recorder.stream)

    override val errors: Flow<Exception>
        field = MutableSharedFlow<Exception>()

    override suspend fun setup(): Result<Unit> = runCatching {
        val model = selectedModelProvider.getSelectedModel()
            .getOrThrow()

        val modelPath = model.modelPath
            ?: throw IllegalStateException("Invalid model cannot use it ")

        val language = model.supportedLanguages.let {
            if (it.contains("*")) null
            else it.firstOrNull()
        }
        // transcription engine setup
        val request = TranscriberConfig(modelPath, language)
        transcriber.setConfig(request, TranscriberEngine.ZIP_FORMER)
    }


    override suspend fun stop(): Result<Unit> {
        val newPath = withContext(dispatchers.io) { _recorder.stop() }
        // TODO: handle saving the recording
        return Result.success(Unit)
    }

    override suspend fun start(): Result<Unit> = runCatching { _recorder.start() }
    override suspend fun pause(): Result<Unit> = runCatching { _recorder.pause() }
    override suspend fun resume(): Result<Unit> = runCatching { _recorder.resume() }
    override suspend fun cancel(): Result<Unit> = runCatching { _recorder.cancel() }

    override fun close() {
        // transcription engine cleanup
        transcriber.setConfig(null)
        // release the recorder
        _recorder.release()
        // cleans up the recorder scope
        if (_scope.isActive) _scope.cancel()
    }
}
