package com.sam.talkdraft.common.model

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

    fun toShortArray(): ShortArray {
        val shortArray = ShortArray(size)
        for (i in array.indices) {
            val sample = array[i].coerceIn(-1.0f, 1.0f)
            shortArray[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return shortArray
    }

    companion object {
        fun wrap(array: FloatArray, readSize: Int): ReadOnlyFloatBuffer {
            return ReadOnlyFloatBuffer(array, readSize.coerceAtLeast(0))
        }

        fun empty(): ReadOnlyFloatBuffer = ReadOnlyFloatBuffer(floatArrayOf(), 0)
    }
}
