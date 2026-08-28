package com.sam.talkdraft.transcription_android.models

data class WhisperState(
    val fullText: String,
    val segment: String? = null,
)
