package com.sam.talkdraft.transcription.domain.model

data class VoiceDetectionResult(
    val probability: Float,
    val isSpeech: Boolean,
)
