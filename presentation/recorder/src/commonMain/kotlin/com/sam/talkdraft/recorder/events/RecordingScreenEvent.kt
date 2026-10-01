package com.sam.talkdraft.recorder.events

internal sealed interface RecordingScreenEvent {
    data object StartRecording : RecordingScreenEvent
    data object StopRecording : RecordingScreenEvent
    data object OnCancelRecording : RecordingScreenEvent
    data object OnResetRecording : RecordingScreenEvent

    // save the transcription
    data object OnSaveTranscription : RecordingScreenEvent

    // permissions and settings
    data object RequestRecordAudioPermission : RecordingScreenEvent
    data object RequestOpenSettings : RecordingScreenEvent
}
