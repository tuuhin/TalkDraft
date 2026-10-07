package com.sam.talkdraft.feature_recorder

import com.sam.talkdraft.common.model.ReadOnlyFloatBuffer
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.feature_recorder.exceptions.InvalidModelException
import com.sam.talkdraft.feature_recorder.exceptions.ModelStartTimeoutException
import com.sam.talkdraft.model_manager.domain.repository.ISelectedTranscriptionModelStore
import com.sam.talkdraft.recorder.domain.IVoiceRecorderWithByteReader
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.recorder_visualizer.domain.IAudioDynamicVisualizer
import com.sam.talkdraft.transcription.domain.ITranscriberResultsProvider
import com.sam.talkdraft.transcription.domain.model.TranscriberConfig
import com.sam.talkdraft.transcription.domain.model.TranscriberEngine
import com.sam.talkdraft.transcription.domain.model.TranscriptionResult
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okio.Path
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

@Factory(binds = [ISimpleVoiceRecorder::class])
class SimpleVoiceRecorderImpl(
    private val dispatchers: IPlatformCoroutineDispatchers,
    private val transcriber: ITranscriberResultsProvider,
    private val selectedModelProvider: ISelectedTranscriptionModelStore,
) : ISimpleVoiceRecorder, KoinComponent {

    private val _scope = CoroutineScope(dispatchers.default + SupervisorJob())

    private val _recorder: IVoiceRecorderWithByteReader by inject(parameters = { parametersOf(_scope) })
    private val _ampsReader: IAudioDynamicVisualizer by inject(parameters = { parametersOf(_recorder) })

    override val recorderState: Flow<RecorderState> = _recorder.state
    override val elapsedTime: Flow<Duration> = _recorder.elapsedTime

    override val waveform: Flow<ReadOnlyFloatBuffer?> = _ampsReader.waveformFlow(30)

    override val transcriptionResult: Flow<TranscriptionResult> = transcriber
        .transcribe(_recorder.stream)
        .onEach { result ->
            if (result !is TranscriptionResult.Success) return@onEach
            if (!result.isBlockResult) return@onEach
            // lock the array
            _lock.withLock { _segmentsCollector.add(result) }
        }
        .shareIn(
            scope = _scope,
            started = SharingStarted.WhileSubscribed(10_000L),
            1,
        )

    private val _errorChannel = Channel<Exception>(capacity = Channel.CONFLATED)
    override val errors: Flow<Exception> = _errorChannel.receiveAsFlow()

    private var _recordingPath: Path? = null
    private val _lock = Mutex()
    private val _segmentsCollector = ArrayList<TranscriptionResult.Success>()

    override suspend fun setup(): Result<Unit> = runCatching {
        // selects the model provider
        val model = selectedModelProvider.getSelectedModel()
            .getOrThrow()

        // selects the model engine
        // TODO: Need to know how the model engine will be added
        val engine = TranscriberEngine.ZIP_FORMER

        if (!model.isAvailable) throw InvalidModelException()
        val modelPath = model.modelPath ?: throw InvalidModelException()

        // sets up what languages can be used
        val language = model.supportedLanguages
            .filterNot { it == "*" }
            .firstOrNull()

        // transcription engine setup
        val request = TranscriberConfig(modelPath, language)
        transcriber.setConfig(request, engine)
        // reset collected segments
        _lock.withLock { _segmentsCollector.clear() }
    }


    override suspend fun stop(): Result<Unit> = runCatching {
        val newPath = withContext(dispatchers.io) { _recorder.stop() }
        _recordingPath = newPath.getOrThrow()
        // reset the transcriber will push out what the transcriber is holding if any
        transcriber.reset()
    }

    override suspend fun start(): Result<Unit> = runCatching {
        // reset the locks on start
        _lock.withLock { _segmentsCollector.clear() }
        _recordingPath = null
        // now to start it we need to ensure the transcriber is on
        try {
            withTimeout(10.seconds) {
                transcriber.isTranscriberReady.first { isReady -> isReady }
            }
        } catch (_: TimeoutCancellationException) {
            // failed to start the model
            throw ModelStartTimeoutException()
        }
        // then start the recorded
        _recorder.start()
    }

    override suspend fun cancel(): Result<Unit> = runCatching {
        // cancel the recorder first
        // if the recorder is running
        if (_recorder.state.value == RecorderState.RECORDING)
            _recorder.cancel()
        // clear the transcriptor engine
        transcriber.clearConfig()
        // clear the segments
        _lock.withLock { _segmentsCollector.clear() }
    }

    override suspend fun onSave(): Result<Unit> = runCatching {
        try {
            // decided to save the transcription results
            val path = _recordingPath ?: throw IllegalStateException("Missing model path to save")
            val segmentBlocks = _lock.withLock { _segmentsCollector.toList() }

            // Save the segments and ensure the recording path is copied to a corrected
            // file path deleted then
            // TODO: Save the segments and path

        } finally {
            transcriber.clearConfig()
            _recordingPath = null
        }
    }

    override fun close() {
        // clear the config if it's an unexpected close
        transcriber.clearConfig()
        // release the recorder
        _recorder.release()
        // clear the segments
        _segmentsCollector.clear()
        // cleans up the recorder scope
        if (_scope.isActive) _scope.cancel()
    }
}
