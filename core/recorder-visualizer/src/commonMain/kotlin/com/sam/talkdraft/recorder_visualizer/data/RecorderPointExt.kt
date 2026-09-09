package com.sam.talkdraft.recorder_visualizer.data

import com.sam.talkdraft.recorder.domain.models.BufferedAudioBlock

internal fun Sequence<BufferedAudioBlock>.normalize(max: Int, min: Int): Sequence<BufferedAudioBlock> {
    val range = (max - min).let { diff -> if (diff <= 0) 1f else diff.toFloat() }
    val minFloat = min.toFloat()

    return map { point ->
        val normalized = ((point.rmsValue - minFloat) / range).coerceIn(0f, 1f)
        point.copy(rmsValue = normalized)
    }
}

internal fun Sequence<BufferedAudioBlock>.smoothen(factor: Float = 0.3f): Sequence<BufferedAudioBlock> {
    var prev = 0f
    return map { point ->
        prev = lerp(prev, point.rmsValue, factor)
        point.copy(rmsValue = prev)
    }
}

internal fun Sequence<BufferedAudioBlock>.padListWithExtra(
    bufferSize: Int,
    extra: Int = 10,
): Sequence<BufferedAudioBlock> = sequence {

    val seen = mutableSetOf<Long>()
    var size = 0
    var lastTime = 0L

    // Yield incoming elements while keeping track of size and time
    for (point in this@padListWithExtra) {
        if (seen.add(point.timeInMillis)) {
            yield(point)
            size++
            lastTime = point.timeInMillis
        }
    }

    // Compute missing count relative to target buffer size
    val differences = bufferSize - size
    val amount = if (differences > 0) differences + extra else extra

    // Yield padded points with isPaddingPoint = true flag set
    for (i in 1..amount) {
        val timeInMillis = lastTime + (i * bufferSize)
        if (seen.add(timeInMillis)) {
            yield(
                BufferedAudioBlock(
                    timeInMillis = timeInMillis,
                    rmsValue = 0f,
                    isPaddingPoint = true,
                ),
            )
        }
    }
}

internal fun Sequence<BufferedAudioBlock>.toProperSequence(eachBlockSize: Int): Sequence<BufferedAudioBlock> {
    return sequence {
        val iterator = this@toProperSequence.iterator()
        if (!iterator.hasNext()) return@sequence

        var expected = iterator.next()
        yield(expected)

        var start = expected.timeInMillis + eachBlockSize

        for (actualPoint in iterator) {
            while (start < actualPoint.timeInMillis) {
                yield(
                    expected.copy(
                        timeInMillis = start,
                        isPaddingPoint = true,
                    ),
                )
                start += eachBlockSize
            }
            yield(actualPoint)
            expected = actualPoint
            start = actualPoint.timeInMillis + eachBlockSize
        }
    }
}

/**
 * Linear interpolation: Returns a point between v0 and v1 at parameter t.
 * Formula: v0 + t * (v1 - v0) == (1 - t) * v0 + t * v1
 */
private fun lerp(v0: Float, v1: Float, t: Float): Float {
    return (1f - t) * v0 + t * v1
}
