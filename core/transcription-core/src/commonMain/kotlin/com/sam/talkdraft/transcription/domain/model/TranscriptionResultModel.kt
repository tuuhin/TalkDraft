package com.sam.talkdraft.transcription.domain.model

data class TranscriptionResultModel(
    val text: String = "",
    val segments: List<TranscriptionSegmentModel> = emptyList(),
)
