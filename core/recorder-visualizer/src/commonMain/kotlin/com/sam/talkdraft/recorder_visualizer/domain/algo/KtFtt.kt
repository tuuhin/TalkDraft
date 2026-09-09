package com.sam.talkdraft.recorder_visualizer.domain.algo

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.koin.core.annotation.Singleton

@Singleton
internal class KtFtt {

    private val cosTable = FloatArray(MAX_FFT_SIZE)
    private val sinTable = FloatArray(MAX_FFT_SIZE)

    init {
        for (i in 0 until MAX_FFT_SIZE) {
            val angle = -2.0 * PI * i / MAX_FFT_SIZE
            cosTable[i] = cos(angle).toFloat()
            sinTable[i] = sin(angle).toFloat()
        }
    }

    fun fft(input: ShortArray, n: Int): FloatArray {
        require(n > 0 && (n and (n - 1)) == 0) { "FFT size n must be a power of 2" }
        require(n <= MAX_FFT_SIZE) { "FFT size n ($n) exceeds precomputed limit ($MAX_FFT_SIZE)" }

        val real = FloatArray(n)
        val imag = FloatArray(n)

        for (i in 0 until n)
            real[i] = if (i < input.size) input[i].toFloat() else 0f

        // Bit-reversal permutation
        var j = 0

        for (i in 0 until n - 1) {

            if (i < j) {
                val tempReal = real[i]
                real[i] = real[j]
                real[j] = tempReal

                val tempImag = imag[i]
                imag[i] = imag[j]
                imag[j] = tempImag
            }
            var k = n shr 1
            while (k <= j) {
                j -= k
                k = k shr 1
            }
            j += k
        }

        // Cooley-Tukey
        var len = 2
        while (len <= n) {
            val halfLen = len shr 1
            val tableStep = MAX_FFT_SIZE / len

            var i = 0
            while (i < n) {
                for (k in 0 until halfLen) {
                    val tableIndex = k * tableStep
                    val wReal = cosTable[tableIndex]
                    val wImag = sinTable[tableIndex]

                    val pos = i + k
                    val match = pos + halfLen

                    val uReal = real[pos]
                    val uImag = imag[pos]

                    val vReal = real[match] * wReal - imag[match] * wImag
                    val vImag = real[match] * wImag + imag[match] * wReal

                    real[pos] = uReal + vReal
                    imag[pos] = uImag + vImag
                    real[match] = uReal - vReal
                    imag[match] = uImag - vImag
                }
                i += len
            }
            len = len shl 1
        }

        // Interleaved Float output.
        val output = FloatArray(n * 2)
        for (i in 0 until n) {
            output[i * 2] = real[i]
            output[i * 2 + 1] = imag[i]
        }

        return output
    }

    companion object {
        private const val MAX_FFT_SIZE = 4096
    }
}
