package com.sam.talkdraft.recorder.model

import androidx.compose.runtime.Stable

@Stable
internal sealed interface RecorderSetupFailedReason {
    data object None : RecorderSetupFailedReason
    data object MissingPermission : RecorderSetupFailedReason
    data class FailedTranscriptionModelSetup(val errorMessage: String?) : RecorderSetupFailedReason
}
