package com.sam.talkdraft.transcription.domain.model

import kotlin.time.Duration

internal sealed interface TranscriptionEngineOutput {
    data class Segment(
        val segmentId: Long,
        val text: String,
        val startTime: Duration = Duration.ZERO,
        val endTime: Duration = Duration.ZERO,
        val isPlaceholder: Boolean = false,
    ) : TranscriptionEngineOutput

    data class InvalidResult(
        val error: TranscriptionError,
        val message: String? = null,
    ) : TranscriptionEngineOutput

    data object Buffering : TranscriptionEngineOutput
}
