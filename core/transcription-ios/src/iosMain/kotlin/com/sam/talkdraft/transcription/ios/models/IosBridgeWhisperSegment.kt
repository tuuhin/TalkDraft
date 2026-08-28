package com.sam.talkdraft.transcription.ios.models

data class IosBridgeWhisperSegment(
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
)
