package com.sam.talkdraft.recorder.model

import androidx.compose.runtime.Immutable
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.transcription.domain.model.TranscriptionState

@Immutable
internal data class RecorderScreenState(
    val isLoaded: Boolean = true,
    val recorderState: RecorderState = RecorderState.IDLE,
    val transcriptions: RealtimeTranscriptions = RealtimeTranscriptions(TranscriptionState.Processing),
    val failedReason: RecorderSetupFailedReason = RecorderSetupFailedReason.None,
)
