package com.sam.talkdraft.transcription_android.models

internal class JniWhisperSegment(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
)
