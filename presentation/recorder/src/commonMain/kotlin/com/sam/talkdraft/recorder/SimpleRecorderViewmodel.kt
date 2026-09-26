package com.sam.talkdraft.recorder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sam.talkdraft.common.model.ReadOnlyFloatBuffer
import com.sam.talkdraft.designsystem.utils.UIEvents
import com.sam.talkdraft.feature_recorder.ISimpleVoiceRecorder
import com.sam.talkdraft.permissions.IAppSettingsProvider
import com.sam.talkdraft.permissions.IPermissionsRequester
import com.sam.talkdraft.permissions.model.IosPermissionStatus
import com.sam.talkdraft.permissions.model.PermissionState
import com.sam.talkdraft.permissions.model.Permissions
import com.sam.talkdraft.recorder.events.RecordingScreenEvent
import com.sam.talkdraft.recorder.model.RecorderScreenState
import com.sam.talkdraft.recorder.model.RecorderSetupFailedReason
import com.sam.talkdraft.transcription.domain.model.TranscriptionState
import kotlin.time.Duration
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
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
) : ViewModel() {

    private val _isLoaded = MutableStateFlow(false)
    private val _failedReason = MutableStateFlow<RecorderSetupFailedReason>(RecorderSetupFailedReason.None)

    private val transcriptions = recorder.transcription
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = TranscriptionState.Idle,
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

    val screenState = combine(
        recorder.recorderState,
        transcriptions,
        _isLoaded,
        _failedReason,
    ) { state, transcriptions, isLoaded, failedReason ->
        RecorderScreenState(
            recorderState = state,
            transcriptions = transcriptions,
            isLoaded = isLoaded,
            failedReason = failedReason,
        )
    }.onStart {
        onSetup()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(10_000L),
        initialValue = RecorderScreenState(),
    )


    val uiEvents: SharedFlow<UIEvents>
        field = MutableSharedFlow<UIEvents>()

    fun onEvent(event: RecordingScreenEvent) = viewModelScope.launch {
        when (event) {
            RecordingScreenEvent.StartRecording -> recorder.start()
            RecordingScreenEvent.StopRecording -> recorder.stop()
            RecordingScreenEvent.OnCancelRecording -> recorder.cancel()
            RecordingScreenEvent.RequestOpenSettings -> permissionRequester.requestPermission(Permissions.RECORD_AUDIO)
            RecordingScreenEvent.RequestRecordAudioPermission -> settingsProvider.openSettings()
        }
    }

    private fun onSetup() = viewModelScope.launch {
        try {
            val permissionState = permissionRequester.checkPermissionStatus(Permissions.RECORD_AUDIO)
            val hasPermission = when (permissionState) {
                is PermissionState.AndroidPermissionState -> permissionState.isGranted
                is PermissionState.IosPermissionState -> permissionState.status == IosPermissionStatus.GRANTED
            }

            if (!hasPermission) {
                // permissions missing failed state
                _failedReason.update { RecorderSetupFailedReason.MissingPermission }
                return@launch
            }

            val result = recorder.setup()
            result.fold(
                onSuccess = { _failedReason.update { RecorderSetupFailedReason.None } },
                onFailure = { err ->
                    // model setup failed
                    val reason = RecorderSetupFailedReason.FailedTranscriptionModelSetup(err.message)
                    _failedReason.update { reason }
                },
            )
        } finally {
            _isLoaded.update { true }
        }
    }


    override fun onCleared() {
        recorder.close()
    }
}
