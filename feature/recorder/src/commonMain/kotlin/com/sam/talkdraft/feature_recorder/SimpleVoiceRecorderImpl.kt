package com.sam.talkdraft.feature_recorder

import com.sam.talkdraft.common.model.ReadOnlyFloatBuffer
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.feature_recorder.exceptions.InvalidModelException
import com.sam.talkdraft.feature_recorder.exceptions.ModelStartTimeoutException
import com.sam.talkdraft.feature_recorder.models.CompleteRecordingSegments
import com.sam.talkdraft.feature_recordings.domain.IRecordingsRepository
import com.sam.talkdraft.feature_recordings.domain.model.CreateRecordingModel
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
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import org.koin.core.annotation.Factory
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

@Factory(binds = [ISimpleVoiceRecorder::class])
internal class SimpleVoiceRecorderImpl(
    private val dispatchers: IPlatformCoroutineDispatchers,
    private val transcriber: ITranscriberResultsProvider,
    private val selectedModelProvider: ISelectedTranscriptionModelStore,
    private val recordingRepo: IRecordingsRepository,
) : ISimpleVoiceRecorder, KoinComponent {

    private val fs = FileSystem.SYSTEM

    private val _scope = CoroutineScope(dispatchers.default + SupervisorJob())

    private val _recorder: IVoiceRecorderWithByteReader by inject(parameters = { parametersOf(_scope) })
    private val _ampsReader: IAudioDynamicVisualizer by inject(parameters = { parametersOf(_recorder) })

    override val recorderState: Flow<RecorderState> = _recorder.state
    override val elapsedTime: Flow<Duration> = _recorder.elapsedTime

    override val waveform: Flow<ReadOnlyFloatBuffer?> = _ampsReader.waveformFlow(30)

    override val realtimeTranscription: StateFlow<TranscriptionResult> = transcriber
        .transcribe(_recorder.stream)
        .onEach { result ->
            // only care about blocks that are successful ,not realtime val has a semget duration
            if (result !is TranscriptionResult.Success || result.isRealtime) return@onEach
            // also completed segments will have a duration
            val duration = result.segmentDuration ?: return@onEach
            val completeSegment = CompleteRecordingSegments(
                segment = result.segment,
                start = duration.start,
                end = duration.endInclusive,
            )
            _completeSegments.update { old -> old + completeSegment }
        }
        .filterNot { state ->
            // filter out if the segment is success and not realtime
            // so blocks are not exposed
            state is TranscriptionResult.Success && !state.isRealtime
        }
        .stateIn(
            scope = _scope,
            started = SharingStarted.WhileSubscribed(10_000L),
            initialValue = TranscriptionResult.Idle,
        )

    private val _completeSegments = MutableStateFlow<List<CompleteRecordingSegments>>(emptyList())
    override val transcriptionSegments = _completeSegments
        .map { completeSegments -> completeSegments.map { it.segment } }
        .stateIn(
            scope = _scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList(),
        )

    private val _errorChannel = Channel<Exception>(capacity = Channel.CONFLATED)
    override val errors: Flow<Exception> = _errorChannel.receiveAsFlow()

    private var _recordingPath: Path? = null
    private var _modelId: Uuid? = null

    override suspend fun setup(): Result<Unit> = runCatching {
        // selects the model provider
        val model = selectedModelProvider.getSelectedModel()
            .getOrThrow()

        // For simple recorder case better to use zip former as whisper on-device
        // is kind a slow but its mostly correct
        // TODO: Need to know how the model engine will be added
        val engine = TranscriberEngine.ZIP_FORMER

        if (!model.isAvailable) throw InvalidModelException()
        val modelPath = model.modelPath ?: throw InvalidModelException()

        // sets up what languages can be used
        val language = model.supportedLanguages
            .filterNot { it == "*" }
            .firstOrNull()

        _modelId = model.id
        // transcription engine setup
        val request = TranscriberConfig(modelPath, language)
        transcriber.setConfig(request, engine)
        // reset collected segments
        _completeSegments.update { emptyList() }
    }


    override suspend fun stop(): Result<Unit> = runCatching {
        val newPath = withContext(dispatchers.io) { _recorder.stop() }
        _recordingPath = newPath.getOrThrow()
        // reset the transcriber will push out what the transcriber is holding if any
        transcriber.reset()
    }

    override suspend fun start(): Result<Unit> = runCatching {
        // reset collected segments
        _completeSegments.update { emptyList() }
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
        _recorder.cancel()
        // clear the transcriptor engine
        transcriber.clearConfig()
        // reset collected segments
        _completeSegments.update { emptyList() }
    }

    override suspend fun onSave(): Result<Unit> = runCatching {
        try {
            // decided to save the transcription results
            val path = _recordingPath ?: throw IllegalStateException("Missing recording file path")
            val modelId = _modelId ?: throw IllegalStateException("Model id absent setup is not called")
            val segmentBlocks = _completeSegments.value
                .sortedBy { it.segment.segmentId }
                .map(CompleteRecordingSegments::toRecordingEnty)

            // Save the segments and ensure the recording path is copied to a corrected
            val createModel = CreateRecordingModel(modelId, path.toString(), segmentBlocks)
            recordingRepo.saveNewRecording(createModel)
        } finally {
            transcriber.clearConfig()
            fieldCleanUp()
        }
    }


    private suspend fun fieldCleanUp() {
        // resets the model id
        _modelId = null
        try {
            // delete the file path
            withContext(dispatchers.io + NonCancellable) {
                val path = _recordingPath ?: return@withContext
                // if the file exists then only delete it
                if (!fs.exists(path)) return@withContext
                fs.delete(path)
            }
        } finally {
            // clear the recording path reference
            _recordingPath = null
        }
    }

    override fun close() {
        // reset the fields
        runBlocking { fieldCleanUp() }
        // clear the config if it's an unexpected close
        transcriber.clearConfig()
        // release the recorder
        _recorder.release()
        // clear the segments
        _completeSegments.update { emptyList() }
        // cleans up the recorder scope
        if (_scope.isActive) _scope.cancel()
    }
}
