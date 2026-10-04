package com.sam.talkdraft.transcription.domain.model

sealed interface TranscriptionResult {

    data object Idle : TranscriptionResult
    data object Ready : TranscriptionResult

    data class Success(
        val segment: TranscriptionSegmentModel,
        val isFinalBlock: Boolean = false,
    ) : TranscriptionResult

    data class Failed(val error: TranscriptionError, val message: String? = null) : TranscriptionResult
}
