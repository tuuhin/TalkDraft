package com.sam.talkdraft.transcription_android.models

import kotlin.time.Duration

data class ZipFormerSegment(
    val segmentId: Long,
    val segment: String,
    val start: Duration = Duration.ZERO,
    val end: Duration = Duration.ZERO,
)
