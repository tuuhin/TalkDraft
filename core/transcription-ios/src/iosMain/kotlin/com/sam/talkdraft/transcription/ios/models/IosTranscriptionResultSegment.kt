package com.sam.talkdraft.transcription.ios.models

data class IosTranscriptionResultSegment(
    val segmentId: Long = 0L,
    val segment: String = "",
    val startTimeMs: Long = 0L,
    val endTimeMs: Long = 0L,
)
