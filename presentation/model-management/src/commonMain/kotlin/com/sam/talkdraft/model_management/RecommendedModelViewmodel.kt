package com.sam.talkdraft.model_management

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.sam.talkdraft.background_jobs.IModelDownloadRegistrar
import com.sam.talkdraft.designsystem.utils.UIEvents
import com.sam.talkdraft.model_downloader.domain.models.DownloadState
import com.sam.talkdraft.model_management.events.SelectedModelScreenEvent
import com.sam.talkdraft.model_management.model.SelectedModelScreenState
import com.sam.talkdraft.model_management.model.UIModelDownloadStatus
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import com.sam.talkdraft.model_manager.domain.repository.IRecommendedModelProvider
import com.sam.talkdraft.model_manager.domain.repository.ITranscriptionModelsRepo
import kotlin.uuid.Uuid
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class RecommendedModelViewmodel(
    private val recommendationProvider: IRecommendedModelProvider,
    private val transcriptionModelRepo: ITranscriptionModelsRepo,
    private val downloadRegistrar: IModelDownloadRegistrar,
) : ViewModel() {

    val uiEvents: SharedFlow<UIEvents>
        field = MutableSharedFlow<UIEvents>()

    private val _recommendedModel = MutableStateFlow<TranscriptionModel?>(null)
    private val _modelDownloadProgress = MutableStateFlow<UIModelDownloadStatus>(UIModelDownloadStatus.Idle)
    private val _isModelLoaded = MutableStateFlow(false)

    val screenState = combine(_recommendedModel, _modelDownloadProgress, _isModelLoaded) { model, progress, isLoaded ->
        SelectedModelScreenState(model = model, downloadStatus = progress, isModelLoaded = isLoaded)
    }.onStart { readAndObserveRecommendedModel() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(10_000L),
            initialValue = SelectedModelScreenState(),
        )

    private var _downloadJobId: Uuid? = null
    private var _observationJob: Job? = null
    private var _isGlobalObservationRunning = false

    fun onEvent(event: SelectedModelScreenEvent) {
        when (event) {
            SelectedModelScreenEvent.CancelDownload -> cancelDownloadJob()
            SelectedModelScreenEvent.DeleteModel -> {}
            SelectedModelScreenEvent.StartDownload -> onStartDownload()
        }
    }

    private fun onStartDownload() {
        val model = _recommendedModel.value ?: return
        cancelObservationJob()

        val jobId = downloadRegistrar.startModelDownload(model)
        _downloadJobId = jobId

        observerModelDownload(jobId)
    }

    private fun cancelDownloadJob() {
        val jobId = _downloadJobId
        if (jobId != null) {
            downloadRegistrar.cancelDownload(jobId)
            _downloadJobId = null
        }
        cancelObservationJob()
        _modelDownloadProgress.update { UIModelDownloadStatus.Idle }
    }

    private fun cancelObservationJob() {
        _observationJob?.cancel()
        _observationJob = null
        _isGlobalObservationRunning = false
    }

    private fun observerModelDownload(jobId: Uuid) {
        cancelObservationJob()
        _modelDownloadProgress.update { UIModelDownloadStatus.Starting }

        _observationJob = viewModelScope.launch {
            downloadRegistrar.observerDownloadStatus(jobId)
                .catch { err -> Logger.w(tag = TAG, throwable = err) { "FAILED TO READ JOB FLOW" } }
                .collect { status ->
                    updateUiDownloadStatus(status.state)
                    checkAndClearTerminalState(status.state, workId = jobId)
                }
        }
    }

    private fun startModelObservation(model: TranscriptionModel) {
        if (_isGlobalObservationRunning || _downloadJobId != null) {
            Logger.d(tag = TAG) { "OBSERVATION ALREADY RUNNING OR DOWNLOAD JOB ACTIVE" }
            return
        }

        cancelObservationJob()
        _observationJob = viewModelScope.launch {
            _isGlobalObservationRunning = true
            try {
                downloadRegistrar.observerDownloadStatus(model)
                    .catch { err -> Logger.w(tag = TAG, throwable = err) { "FAILED TO READ GLOBAL FLOW" } }
                    .collect { (workId, status) ->
                        if (status == null) return@collect
                        updateUiDownloadStatus(status.state)
                        checkAndClearTerminalState(status.state, workId)
                    }
            } finally {
                _isGlobalObservationRunning = false
            }
        }
    }

    private fun checkAndClearTerminalState(state: DownloadState, workId: Uuid) {
        // Clear active ID once the job succeeds or fails
        _downloadJobId = when (state) {
            is DownloadState.Success, is DownloadState.Failed -> null
            is DownloadState.Initiated, is DownloadState.Downloading -> workId
            else -> return
        }

    }

    private fun updateUiDownloadStatus(state: DownloadState) {
        val uiStatus = when (state) {
            DownloadState.Initiated -> UIModelDownloadStatus.Starting
            is DownloadState.Downloading ->
                UIModelDownloadStatus.Downloading(progress = { state.progress / 100 })

            is DownloadState.Failed ->
                UIModelDownloadStatus.Failed(state.message ?: "Unknown reason")

            DownloadState.Success -> UIModelDownloadStatus.Success
            DownloadState.Verifying -> UIModelDownloadStatus.Verifying
        }
        _modelDownloadProgress.update { uiStatus }
    }

    private fun readAndObserveRecommendedModel() = viewModelScope.launch {
        val model = recommendationProvider.recommendedModel().getOrNull()
            ?: return@launch

        transcriptionModelRepo.readModelAsFlow(model.id)
            .catch { err -> Logger.w(tag = TAG, throwable = err) { "FAILED TO READ MODEL FLOW" } }
            .collect { model ->
                if (model != null) startModelObservation(model)
                _recommendedModel.update { model }
                _isModelLoaded.update { true }
            }
    }

    override fun onCleared() {
        cancelObservationJob()
    }

    companion object {
        private const val TAG = "MODEL_RECOMMENDATION_VIEWMODEL"
    }
}
