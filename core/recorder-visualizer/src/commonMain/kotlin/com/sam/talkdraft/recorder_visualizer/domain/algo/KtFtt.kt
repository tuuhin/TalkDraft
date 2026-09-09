package com.sam.talkdraft.recorder_visualizer.domain.algo

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.koin.core.annotation.Singleton

@Singleton
internal class KtFtt {

    // Precomputed Sine and Cosine lookup tables for phase angles
    private val cosTable = FloatArray(MAX_FFT_SIZE)
    private val sinTable = FloatArray(MAX_FFT_SIZE)

    init {
        for (i in 0 until MAX_FFT_SIZE) {
            val angle = -2.0 * PI * i / MAX_FFT_SIZE
            cosTable[i] = cos(angle).toFloat()
            sinTable[i] = sin(angle).toFloat()
        }
    }

    /**
     * Performs forward Radix-2 Cooley-Tukey FFT using precomputed lookup tables.
     *
     * @param input Raw 16-bit PCM samples or time-domain short array (must be power of 2, max 4096).
     * @param n Number of samples to process (must be power of 2).
     * @return ShortArray of size n * 2 (interleaved Real and Imaginary values).
     */
    fun fft(input: ShortArray, n: Int): ShortArray {
        require(n > 0 && (n and (n - 1)) == 0) { "FFT size n must be a power of 2" }
        require(n <= MAX_FFT_SIZE) { "FFT size n ($n) exceeds precomputed limit ($MAX_FFT_SIZE)" }

        val real = FloatArray(n) { if (it < input.size) input[it].toFloat() else 0f }
        val imag = FloatArray(n)

        // Bit-reversal permutation
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempR = real[i]
                real[i] = real[j]
                real[j] = tempR

                val tempI = imag[i]
                imag[i] = imag[j]
                imag[j] = tempI
            }
            var k = n shr 1
            while (k <= j) {
                j -= k
                k = k shr 1
            }
            j += k
        }

        // Cooley-Tukey iterative FFT computation using precomputed tables
        var len = 2
        while (len <= n) {
            val halfLen = len shr 1
            val tableStep = MAX_FFT_SIZE / len

            var i = 0
            while (i < n) {
                for (k in 0 until halfLen) {
                    val tableIdx = k * tableStep
                    val wR = cosTable[tableIdx]
                    val wI = sinTable[tableIdx]

                    val pos = i + k
                    val match = pos + halfLen

                    val uR = real[pos]
                    val uI = imag[pos]

                    val vR = real[match] * wR - imag[match] * wI
                    val vI = real[match] * wI + imag[match] * wR

                    real[pos] = uR + vR
                    imag[pos] = uI + vI

                    real[match] = uR - vR
                    imag[match] = uI - vI
                }
                i += len
            }
            len = len shl 1
        }

        // Interleave Real and Imaginary values into ShortArray output
        val output = ShortArray(n * 2)
        for (idx in 0 until n) {
            output[idx * 2] = real[idx].toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            output[idx * 2 + 1] = imag[idx].toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        return output
    }

    companion object {
        private const val MAX_FFT_SIZE = 4096
    }
}
