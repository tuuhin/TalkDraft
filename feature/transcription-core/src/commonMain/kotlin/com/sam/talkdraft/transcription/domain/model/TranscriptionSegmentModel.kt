package com.sam.talkdraft.transcription.domain.model

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class TranscriptionSegmentModel(
    val text: String,
    val startTimeMs: Duration = 0.seconds,
    val endTime: Duration = 0.seconds,
) {
    val isValid: Boolean
        get() = text.isNotBlank() && endTime - startTimeMs > Duration.ZERO
}
