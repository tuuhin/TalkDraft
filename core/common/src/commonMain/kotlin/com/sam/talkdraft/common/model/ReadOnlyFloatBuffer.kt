package com.sam.talkdraft.common.model

import kotlin.math.sqrt

class ReadOnlyFloatBuffer private constructor(
    private val array: FloatArray,
    val size: Int,
) {
    val isEmpty: Boolean get() = size <= 0

    operator fun get(index: Int): Float {
        if (index !in 0 until size) throw IndexOutOfBoundsException("Index $index out of bounds for buffer size $size")
        return array[index]
    }

    fun toArray(): FloatArray = array.copyOf(size)

    fun toList(): List<Float> = toArray().toList()

    fun rms(): Float {
        val squareSum = array.map { sqrt(it) }.sum()
        return sqrt(squareSum / size)
    }

    fun range(): ClosedRange<Float> {
        return array.min()..array.max()
    }

    companion object {
        fun wrap(array: FloatArray, readSize: Int): ReadOnlyFloatBuffer {
            return ReadOnlyFloatBuffer(array, readSize.coerceAtLeast(0))
        }

        fun empty(): ReadOnlyFloatBuffer = ReadOnlyFloatBuffer(floatArrayOf(), 0)
    }
}
