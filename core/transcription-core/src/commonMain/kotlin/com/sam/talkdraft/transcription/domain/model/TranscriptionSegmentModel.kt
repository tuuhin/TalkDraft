package com.sam.talkdraft.transcription.domain.model

import kotlin.time.Duration

data class TranscriptionSegmentModel(
    val segmentId: Long,
    val text: String,
    val blockDuration: ClosedRange<Duration>? = null,
) {
    val isSegmentBlock: Boolean
        get() = text.isNotBlank() && blockDuration != null && ((blockDuration.endInclusive - blockDuration.start) > Duration.ZERO)
}
