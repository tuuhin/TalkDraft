package com.sam.talkdraft.feature_recordings.domain.model

import kotlin.time.Duration
import kotlin.uuid.Uuid

data class RecordingTranscriptSegment(
    val startDuration: Duration,
    val endDuration: Duration,
    val segment: String,
    val speakerId: Uuid? = null,
)
