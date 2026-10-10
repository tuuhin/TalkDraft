package com.sam.talkdraft.recorder.model

import androidx.compose.runtime.Stable
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.transcription.domain.model.TranscriptionResult
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Stable
internal data class RecorderSheetState(
    val state: RecorderUIState = RecorderUIState(),
    val failedReason: RecorderFailedReason = RecorderFailedReason.None,
    val isModelSetupRunning: Boolean = false,
    val finalizedTranscriptions: ImmutableList<String> = persistentListOf(),
    val isSavingRecording: Boolean = false,
) {

    val recorderState: RecorderState
        get() = state.recorderState

    val transcriptionResult: TranscriptionResult
        get() = state.transcriptions
}
