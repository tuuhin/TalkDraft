package com.sam.talkdraft.transcription_android.models

data class AndroidVadConfig(
    val sampleRate: Int = 16_000,
    val silenceThreshold: Float = 0.3f,
    val minSilenceInSeconds: Float = 0.2f,
    val minSpeechInSeconds: Float = 0.25f,
    val maxSpeechInSeconds: Float = 20.0f,
    val noOfThreads: Int = 1,
    val bufferSeconds: Float = 30.0f,
)
