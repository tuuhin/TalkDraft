package com.sam.talkdraft.recorder.model

import androidx.compose.runtime.Stable
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.transcription.domain.model.TranscriptionResult

@Stable
internal data class RecorderSheetState(
    val state: RecorderUIState = RecorderUIState(),
    val failedReason: RecorderSetupFailedReason = RecorderSetupFailedReason.None,
    val isRecorderReady: Boolean = true,
    val finalizedTranscriptionText: String? = null,
    val isSavingRecording: Boolean = false,
) {

    val recorderState: RecorderState
        get() = state.recorderState

    val transcriptionResult: TranscriptionResult
        get() = state.transcriptions
}
