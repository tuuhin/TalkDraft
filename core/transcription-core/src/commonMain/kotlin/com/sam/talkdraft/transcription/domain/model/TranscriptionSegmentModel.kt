package com.sam.talkdraft.transcription.domain.model

data class TranscriptionSegmentModel(
    val segmentId: Long,
    val text: String,
) {
    val isValid: Boolean
        get() = segmentId > 0

    val isNotBlank: Boolean
        get() = text.isNotBlank()
}
