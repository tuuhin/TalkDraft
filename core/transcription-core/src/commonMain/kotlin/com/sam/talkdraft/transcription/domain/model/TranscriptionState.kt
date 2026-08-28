package com.sam.talkdraft.transcription.domain.model

sealed interface TranscriptionState {

    data object Processing : TranscriptionState
    data class Completed(val result: TranscriptionResultModel) : TranscriptionState
    data class Failed(val error: TranscriptionError) : TranscriptionState
}
