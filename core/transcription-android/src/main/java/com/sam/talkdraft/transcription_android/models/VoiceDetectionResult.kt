package com.sam.talkdraft.transcription_android.models

@JvmInline
value class VoiceDetectionResult(val probability: Float) {
    val isSpeech: Boolean
        get() = probability >= 0.5f
}
