package com.sam.talkdraft.transcription.domain.model

data class TranscriberConfig(
    val modelPath: String,
    val language: String? = null,
)
