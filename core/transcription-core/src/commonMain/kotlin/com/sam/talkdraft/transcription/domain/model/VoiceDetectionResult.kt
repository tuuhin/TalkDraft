package com.sam.talkdraft.transcription.domain.model

internal data class VoiceDetectionResult(
    val probability: Float,
    val isSpeech: Boolean,
)
