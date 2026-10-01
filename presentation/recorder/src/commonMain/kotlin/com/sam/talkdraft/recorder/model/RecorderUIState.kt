package com.sam.talkdraft.recorder.model

import androidx.compose.runtime.Immutable
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.transcription.domain.model.TranscriptionResult

@Immutable
internal data class RecorderUIState(
    val recorderState: RecorderState = RecorderState.IDLE,
    val transcriptions: TranscriptionResult = TranscriptionResult.Idle,
)
