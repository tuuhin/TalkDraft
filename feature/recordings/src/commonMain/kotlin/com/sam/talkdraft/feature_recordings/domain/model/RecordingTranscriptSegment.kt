package com.sam.talkdraft.feature_recordings.domain.model

import kotlin.time.Duration

data class RecordingTranscriptSegment(
    val startDuration: Duration,
    val endDuration: Duration,
    val segment: String,
)
