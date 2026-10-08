package com.sam.talkdraft.transcription_android.punctuation

internal class JniPunctuationConfig(
    val modelPath: String,
    val isOnline: Boolean = true,
    val vocabPath: String? = null,
)
