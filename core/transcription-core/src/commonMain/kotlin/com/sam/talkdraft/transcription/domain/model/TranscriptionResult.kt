package com.sam.talkdraft.transcription.domain.model

import kotlin.time.Duration

sealed interface TranscriptionResult {

    data object Idle : TranscriptionResult
    data object Ready : TranscriptionResult
    data object Listening : TranscriptionResult

    data class Success(
        val segment: TranscriptionSegmentModel,
        val isRealtime: Boolean = true,
        val segmentDuration: ClosedRange<Duration>? = null,
    ) : TranscriptionResult

    data class Failed(val error: TranscriptionError, val message: String? = null) : TranscriptionResult
}
