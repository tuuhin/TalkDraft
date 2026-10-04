package com.sam.talkdraft.transcription.domain.model

import kotlin.time.Duration

data class TranscriptionSegmentModel(
    val segmentId: Long,
    val text: String,
    val durationRange: ClosedRange<Duration> = Duration.ZERO..Duration.ZERO,
) {
    val isValid: Boolean
        get() = text.isNotBlank() && durationRange.endInclusive - durationRange.start > Duration.ZERO
}
