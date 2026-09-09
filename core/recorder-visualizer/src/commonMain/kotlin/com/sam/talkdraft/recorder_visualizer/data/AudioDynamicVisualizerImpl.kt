@file:OptIn(FlowPreview::class)

package com.sam.talkdraft.recorder_visualizer.data

import com.sam.talkdraft.common.model.ReadOnlyFloatBuffer
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.recorder.RecorderConstants
import com.sam.talkdraft.recorder.domain.IVoiceRecorderWithByteReader
import com.sam.talkdraft.recorder_visualizer.domain.IAudioDynamicVisualizer
import com.sam.talkdraft.recorder_visualizer.domain.algo.KtFtt
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.Duration
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
            .sample(delayRate)
            .map { buffer ->
                val rawShorts = buffer.toShortArray()
                val rawSize = rawShorts.size

                if (rawSize == 0) return@map ShortArray(0)

                val paddedSize = if ((rawSize and (rawSize - 1)) == 0) rawSize
                else 1 shl (32 - rawSize.countLeadingZeroBits())

                val fftInput = if (rawShorts.size == paddedSize) rawShorts
                else rawShorts.copyOf(paddedSize)

                ktFtt.fft(fftInput, paddedSize)
            }
            .map { fftData -> fftData.processAndNormalizeFft(barCount = noOfBlocks) }
            .scan(FloatArray(noOfBlocks) { 0.08f }) { previous, target -> smoothBarState(previous, target) }
            .map { array -> ReadOnlyFloatBuffer.wrap(array, array.size) }
            .flowOn(dispatchers.default)
    }

    private fun ShortArray.processAndNormalizeFft(barCount: Int = 50): FloatArray {
        val result = FloatArray(barCount)
        val numComplexBins = size / 2
        if (numComplexBins == 0 || barCount == 0) return result

        val binsPerBar = (numComplexBins / barCount).coerceAtLeast(1)

        for (i in 0 until barCount) {
            var sumSquares = 0.0
            val startBin = i * binsPerBar
            var actualBinsInBar = 0

            for (j in 0 until binsPerBar) {
                val binIndex = startBin + j
                val realIdx = binIndex * 2
                val imagIdx = realIdx + 1

                if (imagIdx < size) {
                    val real = get(realIdx).toDouble()
                    val imag = get(imagIdx).toDouble()
                    sumSquares += (real.pow(2) + imag.pow(2))
                    actualBinsInBar++
                }
            }

            if (actualBinsInBar > 0) {
                val rms = sqrt(sumSquares / actualBinsInBar)
                val normalized = (log10(1.0 + rms) / 4.5).toFloat()
                result[i] = normalized.coerceIn(0.08f, 1.0f)
            } else {
                result[i] = 0.08f
            }
        }
        return result
    }

    private fun smoothBarState(previous: FloatArray, target: FloatArray): FloatArray {
        val smoothed = FloatArray(target.size)
        for (i in target.indices) {
            val prevVal = previous.getOrElse(i) { 0.08f }
            val targetVal = target[i]
            // Fast attack (0.6f) when audio spikes, slow decay (0.2f) when it falls
            val alpha = if (targetVal > prevVal) 0.6f else 0.2f
            smoothed[i] = prevVal + alpha * (targetVal - prevVal)
        }
        return smoothed
    }
}
