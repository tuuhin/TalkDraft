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
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.recorder.events.RecordingScreenEvent
import com.sam.talkdraft.recorder.model.RecorderSetupFailedReason
import com.sam.talkdraft.recorder.model.RecorderSheetState
import com.sam.talkdraft.recorder.model.RecorderUIState
import com.sam.talkdraft.transcription.domain.model.TranscriptionResult
import kotlin.time.Duration
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

    private val _isRecorderReady = MutableStateFlow(false)
    private val _isSavingResult = MutableStateFlow(false)
    private val _failedReason = MutableStateFlow<RecorderSetupFailedReason>(RecorderSetupFailedReason.None)
    private val _fullTranscriptionResult = MutableStateFlow<String?>(null)

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

    // need a Stateflow references otherwise combine will be not activated
    // then combine with the recorderState flow
    private val recorderUIState = recorder.transcriptionResult
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = TranscriptionResult.Idle,
        ).combine(recorder.recorderState) { transcriptionResult, recorderState ->
            RecorderUIState(recorderState, transcriptionResult)
        }.onEach {
            val recorderState = it.recorderState
            val result = it.transcriptions
            Logger.d(tag = "SOME_TAG") { "$recorderState $result" }
            // if its in preparing state reset the value
            if (recorderState == RecorderState.IDLE) {
                _fullTranscriptionResult.update { null }
                return@onEach
            }
            // if transcription is success update the value by appending the new value
            // TODO: replace this with a use case class
            if (result is TranscriptionResult.Success) {
                _fullTranscriptionResult.update { old ->
                    if (old == null) result.segment.text
                    else old + ". " + result.segment.text
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(10_000L),
            initialValue = RecorderUIState(),
        )

    val sheetState = combine(
        recorderUIState,
        _fullTranscriptionResult,
        _isRecorderReady,
        _failedReason,
        _isSavingResult,
    ) { state, fullResult, isLoaded, failedReason, isSaving ->
        RecorderSheetState(
            state = state,
            isRecorderReady = isLoaded,
            finalizedTranscriptionText = fullResult,
            failedReason = failedReason,
            isSavingRecording = isSaving,
        )
    }.onStart {
        prePermissionCheck()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(10_000L),
        initialValue = RecorderSheetState(),
    )


    val uiEvents: SharedFlow<UIEvents>
        field = MutableSharedFlow<UIEvents>()

    fun onEvent(event: RecordingScreenEvent) = viewModelScope.launch {
        when (event) {
            RecordingScreenEvent.StartRecording -> recorder.start()
            RecordingScreenEvent.StopRecording -> recorder.stop()
            RecordingScreenEvent.OnCancelRecording, RecordingScreenEvent.OnResetRecording -> recorder.cancel()
            RecordingScreenEvent.RequestOpenSettings -> permissionRequester.requestPermission(Permissions.RECORD_AUDIO)
            RecordingScreenEvent.RequestRecordAudioPermission -> settingsProvider.openSettings()
            RecordingScreenEvent.OnSaveTranscription -> onSaveTranscription()
        }
    }

    private fun prePermissionCheck() = viewModelScope.launch {
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
            _isRecorderReady.update { true }
        }
    }

    private fun onSaveTranscription() = viewModelScope.launch {
        _isSavingResult.update { true }
        recorder.onSave().fold(
            onSuccess = {
                // TODO: mark onboarding completed
                _isSavingResult.update { false }
                // add the details screen somewhere
                navigator.updateBackStack(listOf(NavDestinations.HomeScreen))
            },
            onFailure = {},
        )
    }


    override fun onCleared() {
        // always close the recorder in any case
        recorder.close()
    }
}
