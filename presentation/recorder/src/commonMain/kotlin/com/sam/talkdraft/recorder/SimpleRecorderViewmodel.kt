package com.sam.talkdraft.recorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.model.ReadOnlyFloatBuffer
import com.sam.talkdraft.designsystem.utils.UIEvents
import com.sam.talkdraft.feature_recorder.ISimpleVoiceRecorder
import com.sam.talkdraft.navigation.NavDestinations
import com.sam.talkdraft.navigation.navigator.AppNavigator
import com.sam.talkdraft.permissions.IAppSettingsProvider
import com.sam.talkdraft.permissions.IPermissionsRequester
import com.sam.talkdraft.permissions.model.IosPermissionStatus
import com.sam.talkdraft.permissions.model.PermissionState
import com.sam.talkdraft.permissions.model.Permissions
import com.sam.talkdraft.recorder.events.RecordingScreenEvent
import com.sam.talkdraft.recorder.model.RecorderFailedReason
import com.sam.talkdraft.recorder.model.RecorderSheetState
import com.sam.talkdraft.recorder.model.RecorderUIState
import kotlin.time.Duration
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class SimpleRecorderViewmodel(
    private val recorder: ISimpleVoiceRecorder,
    private val permissionRequester: IPermissionsRequester,
    private val settingsProvider: IAppSettingsProvider,
    private val navigator: AppNavigator,
) : ViewModel() {

    private val _isRecorderSetupRunning = MutableStateFlow(false)
    private val _isSavingResult = MutableStateFlow(false)
    private val _failedReason = MutableStateFlow<RecorderFailedReason>(RecorderFailedReason.None)

    private val _recorderUIState: StateFlow<RecorderUIState> = combine(
        flow = recorder.recorderState,
        flow2 = recorder.realtimeTranscription,
        transform = ::RecorderUIState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(10_000L),
        initialValue = RecorderUIState(),
    )

    val recorderDuration: StateFlow<Duration> = recorder.elapsedTime
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(10_000),
            initialValue = Duration.ZERO,
        )

    val recorderWaveform: StateFlow<ReadOnlyFloatBuffer> = recorder.waveform
        .filterNotNull()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = ReadOnlyFloatBuffer.wrap(floatArrayOf(), 0),
        )

    val uiState: StateFlow<RecorderSheetState> = combine(
        _recorderUIState, _failedReason,
        _isRecorderSetupRunning,
        recorder.transcriptionSegments, _isSavingResult,
    ) { uiState, failedReason, isSetupRunning, fullBlock, isSavingResult ->
        RecorderSheetState(
            state = uiState,
            failedReason = failedReason,
            isModelSetupRunning = isSetupRunning,
            finalizedTranscriptions = fullBlock.map { it.text }.toImmutableList(),
            isSavingRecording = isSavingResult,
        )
    }.onStart {
        checkRecordAudioPermission()
    }.onEach { Logger.d(tag = "SOME_TAG") { "$it" } }
        .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(10_000L),
        initialValue = RecorderSheetState(),
    )

    val uiEvents: SharedFlow<UIEvents>
        field = MutableSharedFlow<UIEvents>()

    fun onEvent(event: RecordingScreenEvent) = viewModelScope.launch {
        when (event) {
            RecordingScreenEvent.StartRecording -> onSetupAndStart()
            RecordingScreenEvent.StopRecording -> recorder.stop()
            RecordingScreenEvent.OnCancelRecording, RecordingScreenEvent.OnResetRecording -> recorder.cancel()
            RecordingScreenEvent.RequestOpenSettings -> permissionRequester.requestPermission(Permissions.RECORD_AUDIO)
            RecordingScreenEvent.RequestRecordAudioPermission -> settingsProvider.openSettings()
            RecordingScreenEvent.OnSaveTranscription -> onSaveTranscription()
        }
    }

    fun onSetupAndStart() = viewModelScope.launch {
        try {
            _isRecorderSetupRunning.update { true }
            // start recorder setup
            val result = recorder.setup()
            result.fold(
                onSuccess = { _failedReason.update { RecorderFailedReason.None } },
                onFailure = { err ->
                    // model setup failed
                    val reason = RecorderFailedReason.GenericError(err.message)
                    _failedReason.update { reason }
                    return@launch
                },
            )
            // start the recorder
            recorder.start()
        } finally {
            // recorder setup completed
            _isRecorderSetupRunning.update { false }
        }
    }

    private fun checkRecordAudioPermission() = viewModelScope.launch {
        val permissionState = permissionRequester.checkPermissionStatus(Permissions.RECORD_AUDIO)
        val hasPermission = when (permissionState) {
            is PermissionState.AndroidPermissionState -> permissionState.isGranted
            is PermissionState.IosPermissionState -> permissionState.status == IosPermissionStatus.GRANTED
        }

        if (hasPermission) return@launch
        // permissions missing failed state
        _failedReason.update { RecorderFailedReason.MissingPermission }
    }

    private fun onSaveTranscription() = viewModelScope.launch {
        _isSavingResult.update { true }
        val saveResult = recorder.onSave()
        saveResult.fold(
            onSuccess = {
                // TODO: MARK ONBOARDING COMPLETED AND MARK ANALYTICS TOO
                _isSavingResult.update { false }
                // add the details screen somewhere
                navigator.updateBackStack(listOf(NavDestinations.HomeScreen))
            },
            onFailure = { err ->
                val message = err.message
                _failedReason.update { RecorderFailedReason.GenericError(message) }
            },
        )
    }

    override fun onCleared() {
        // always close the recorder in any case
        recorder.close()
    }
}
