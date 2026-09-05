package com.sam.talkdraft.transcription_android.models

data class WhisperState(
    val text: String,
    val segments: List<WhisperSegment> = emptyList(),
)

data class WhisperSegment(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
)
