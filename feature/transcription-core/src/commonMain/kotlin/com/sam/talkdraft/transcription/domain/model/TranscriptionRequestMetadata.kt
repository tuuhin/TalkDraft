package com.sam.talkdraft.transcription.domain.model

data class TranscriptionRequestMetadata(
    val modelPath: String,
    val language: String? = null,
)
