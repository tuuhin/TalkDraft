package com.sam.talkdraft.transcription_android.models

import kotlin.time.Duration

data class AndroidWhisperSegment(
    val text: String = "",
    val startTime: Duration,
    val endTime: Duration,
)
