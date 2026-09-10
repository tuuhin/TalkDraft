package com.sam.talkdraft.transcription_android.models

sealed class ProcessingState {
    data object Buffering : ProcessingState()
    data object Success : ProcessingState()
    data class Error(val errorCode: WhisperErrorCode?) : ProcessingState()
}
