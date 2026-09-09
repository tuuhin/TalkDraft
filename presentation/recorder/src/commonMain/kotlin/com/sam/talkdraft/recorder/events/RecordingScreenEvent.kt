package com.sam.talkdraft.recorder.events

internal sealed interface RecordingScreenEvent {
    data object StartRecording : RecordingScreenEvent
    data object StopRecording : RecordingScreenEvent
    data object OnCancelRecording : RecordingScreenEvent
    data object RequestRecordAudioPermission : RecordingScreenEvent
    data object RequestOpenSettings : RecordingScreenEvent
}
