package com.sam.talkdraft.transcription.domain.model

import com.sam.talkdraft.common.model.ReadOnlyFloatBuffer
import kotlin.time.Duration

internal class TimedVoiceDetectionSegment(
    val timedDuration: ClosedRange<Duration>,
    val samples: ReadOnlyFloatBuffer,
)
