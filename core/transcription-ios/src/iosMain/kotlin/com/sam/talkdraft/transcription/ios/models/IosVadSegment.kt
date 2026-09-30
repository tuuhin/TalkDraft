package com.sam.talkdraft.transcription.ios.models

import kotlin.time.Duration

data class IosVadSegment(
    val startSample: Duration,
    val endSample: Duration,
    val samples: FloatArray,
) {

    val duration: Duration
        get() = endSample - startSample

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as IosVadSegment

        if (startSample != other.startSample) return false
        if (endSample != other.endSample) return false
        if (!samples.contentEquals(other.samples)) return false
        if (duration != other.duration) return false

        return true
    }

    override fun hashCode(): Int {
        var result = startSample.hashCode()
        result = 31 * result + endSample.hashCode()
        result = 31 * result + samples.contentHashCode()
        result = 31 * result + duration.hashCode()
        return result
    }
}
