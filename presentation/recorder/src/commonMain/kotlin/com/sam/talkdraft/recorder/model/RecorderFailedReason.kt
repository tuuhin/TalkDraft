package com.sam.talkdraft.recorder.model

import androidx.compose.runtime.Stable

@Stable
internal sealed class RecorderFailedReason {
    data object None : RecorderFailedReason()
    data object MissingPermission : RecorderFailedReason()
    data class GenericError(val errorMessage: String?) : RecorderFailedReason()
}
