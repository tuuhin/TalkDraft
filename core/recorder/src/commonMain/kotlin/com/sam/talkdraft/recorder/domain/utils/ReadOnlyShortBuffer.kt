package com.sam.talkdraft.recorder.domain.utils

class ReadOnlyShortBuffer private constructor(
    private val array: ShortArray,
    val size: Int,
) {
    val isEmpty: Boolean get() = size <= 0

    operator fun get(index: Int): Short {
        if (index !in 0 until size) throw IndexOutOfBoundsException("Index $index out of bounds for buffer size $size")
        return array[index]
    }

    fun toShortArray(): ShortArray = array.copyOf(size)

    companion object {
        internal fun wrap(array: ShortArray, readSize: Int): ReadOnlyShortBuffer {
            return ReadOnlyShortBuffer(array, readSize.coerceAtLeast(0))
        }

        internal fun empty(): ReadOnlyShortBuffer = ReadOnlyShortBuffer(shortArrayOf(), 0)
    }
}
