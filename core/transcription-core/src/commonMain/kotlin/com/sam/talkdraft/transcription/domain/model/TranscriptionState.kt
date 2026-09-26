package com.sam.talkdraft.transcription.domain.model

sealed interface TranscriptionState {

    data object Idle : TranscriptionState
    data object Preparing : TranscriptionState
    data object Ready : TranscriptionState

    data class Success(
        val text: String,
        val segments: List<TranscriptionSegmentModel>,
    ) : TranscriptionState

    data class Failed(val error: TranscriptionError, val message: String? = null) : TranscriptionState
}
