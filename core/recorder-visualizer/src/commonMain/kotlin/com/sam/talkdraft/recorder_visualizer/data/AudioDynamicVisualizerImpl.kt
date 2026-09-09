@file:OptIn(FlowPreview::class)

package com.sam.talkdraft.recorder_visualizer.data

import com.sam.talkdraft.common.model.ReadOnlyFloatBuffer
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.recorder.RecorderConstants
import com.sam.talkdraft.recorder.domain.IVoiceRecorderWithByteReader
import com.sam.talkdraft.recorder_visualizer.domain.IAudioDynamicVisualizer
import com.sam.talkdraft.recorder_visualizer.domain.algo.KtFtt
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.scan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam

@Factory(binds = [IAudioDynamicVisualizer::class])
internal class AudioDynamicVisualizerImpl(
    @InjectedParam private val recorder: IVoiceRecorderWithByteReader,
    private val ktFtt: KtFtt,
    private val dispatchers: IPlatformCoroutineDispatchers,
    private val delayRate: Duration = RecorderConstants.STOPWATCH_DELAY_RATE,
) : IAudioDynamicVisualizer {

    override fun waveformFlow(noOfBlocks: Int): Flow<ReadOnlyFloatBuffer> {
        return recorder.stream
            .buffer(Channel.CONFLATED)
            .sample(minOf(60.milliseconds, delayRate))
            .map { buffer ->
                val rawShorts = buffer.toShortArray()
                if (rawShorts.isEmpty()) return@map FloatArray(noOfBlocks)

                val paddedSize = if ((rawShorts.size and (rawShorts.size - 1)) == 0) rawShorts.size
                else 1 shl (32 - rawShorts.size.countLeadingZeroBits())

                val fftInput = if (rawShorts.size == paddedSize) rawShorts
                else rawShorts.copyOf(paddedSize)
                ktFtt.fft(fftInput, paddedSize)
            }
            .map { fftData -> fftData.processAndNormalizeFft(barCount = noOfBlocks) }
            .scan(FloatArray(noOfBlocks)) { previous, target -> smoothBarState(previous = previous, target = target) }
            .map { values -> ReadOnlyFloatBuffer.wrap(values, values.size) }
            .flowOn(dispatchers.default)
    }

    private fun FloatArray.processAndNormalizeFft(barCount: Int = 50): FloatArray {
        val magnitudes = FloatArray(barCount)
        if (barCount <= 0 || size < 4) return magnitudes

        val fftSize = size / 2
        val nyquistBin = fftSize / 2
        if (nyquistBin <= 1) return magnitudes

        val sampleRate = 16_000f
        val minFrequency = 60f
        val maxFrequency = 8_000f

        val minBin = (minFrequency * fftSize / sampleRate).toInt().coerceIn(1, nyquistBin - 1)
        val maxBin = (maxFrequency * fftSize / sampleRate).toInt().coerceIn(minBin + 1, nyquistBin)

        val totalBins = maxBin - minBin
        if (totalBins <= 0) return magnitudes

        var maxEnergy = 0.0
        for (bin in minBin until maxBin) {
            val real = this[bin * 2].toDouble()
            val imag = this[bin * 2 + 1].toDouble()
            val energy = real * real + imag * imag
            if (energy > maxEnergy) maxEnergy = energy
        }
        if (maxEnergy <= 0.0) return magnitudes

        for (bar in 0 until barCount) {
            val startBin = minBin + (bar * totalBins / barCount)
            val endBin = minBin + ((bar + 1) * totalBins / barCount)
            if (startBin >= endBin) continue

            var energy = 0.0
            for (bin in startBin until endBin) {
                val real = this[bin * 2].toDouble()
                val imag = this[bin * 2 + 1].toDouble()
                energy += real * real + imag * imag
            }

            val meanEnergy = energy / (endBin - startBin)
            val normalized = (meanEnergy / maxEnergy).coerceIn(0.0, 1.0)

            magnitudes[bar] = sqrt(normalized).toFloat().coerceIn(0f, 1f)
        }

        return magnitudes.centerMirror()
    }

    private fun FloatArray.centerMirror(): FloatArray {
        val n = size
        val output = FloatArray(n)
        val center = n / 2
        var left = center - 1
        var right = center

        for (i in indices) {
            val value = this[i]
            if (i % 2 == 0 && right < n) {
                output[right] = value
                right++
            } else if (left >= 0) {
                output[left] = value
                left--
            }
        }
        return output
    }

    private fun smoothBarState(previous: FloatArray, target: FloatArray): FloatArray {
        val result = FloatArray(target.size)

        for (i in target.indices) {
            val previousValue = previous.getOrElse(i) { 0f }
            val targetValue = target[i].coerceIn(0f, 1f)
            val alpha = if (targetValue > previousValue) 0.65f else 0.25f
            result[i] = previousValue + alpha * (targetValue - previousValue)
        }

        return result
    }
}
