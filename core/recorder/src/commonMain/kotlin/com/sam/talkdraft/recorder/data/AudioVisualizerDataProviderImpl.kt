package com.sam.talkdraft.recorder.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.recorder.RecorderConstants
import com.sam.talkdraft.recorder.data.mapper.normalize
import com.sam.talkdraft.recorder.data.mapper.padListWithExtra
import com.sam.talkdraft.recorder.data.mapper.smoothen
import com.sam.talkdraft.recorder.data.mapper.toProperSequence
import com.sam.talkdraft.recorder.domain.IAudioVisualizerProvider
import com.sam.talkdraft.recorder.domain.IVoiceRecorderWithByteReader
import com.sam.talkdraft.recorder.domain.models.RecorderPoint
import com.sam.talkdraft.recorder.domain.utils.ReadOnlyShortBuffer
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.scan
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam

private const val TAG = "AudioVisualizerProvider"

@OptIn(
    ExperimentalAtomicApi::class,
    FlowPreview::class,
)
@Factory(binds = [IAudioVisualizerProvider::class])
internal class AudioVisualizerDataProviderImpl(
    @InjectedParam private val recorder: IVoiceRecorderWithByteReader,
    private val dispatchers: IPlatformCoroutineDispatchers,
    delayRate: Duration = RecorderConstants.STOPWATCH_DELAY_RATE,
    private val bufferSize: Int = RecorderConstants.VISUALIZER_BUFFER_SIZE,
) : IAudioVisualizerProvider {

    private data class VisualizerState(
        val buffer: ArrayDeque<RecorderPoint> = ArrayDeque(),
        var rangeMin: Int = 0,
        var rangeMax: Int = 100,
    )

    private val slowedRmsPoints: Flow<Float> = recorder.stream
        .buffer(Channel.CONFLATED)
        .onStart {
            emit(ReadOnlyShortBuffer.empty())
        }
        .map { buffer ->
            if (buffer.isEmpty) 0f else buffer.rms()
        }
        .flowOn(dispatchers.io)
        .sample(delayRate)


    @OptIn(ExperimentalCoroutinesApi::class)
    override val dataPoints: Flow<Sequence<RecorderPoint>>
        get() = combine(slowedRmsPoints, recorder.elapsedTime) { rms, t -> rms to t.inWholeMilliseconds }
            .scan(VisualizerState()) { state, (newValue, stopWatchTime) ->
                val entry = (stopWatchTime / bufferSize) * bufferSize
                val newPoint = RecorderPoint(timeInMillis = entry, rmsValue = newValue)

                if (state.buffer.lastOrNull()?.timeInMillis != newPoint.timeInMillis) {
                    state.buffer.addLast(newPoint)
                }

                if (state.buffer.size >= bufferSize * 3) {
                    repeat(bufferSize) {
                        if (state.buffer.isNotEmpty()) state.buffer.removeFirst()
                    }
                }

                state.rangeMin = min(state.rangeMin, newValue.toInt())
                state.rangeMax = max(state.rangeMax, newValue.toInt())

                state
            }
            .mapLatest { state ->
                state.buffer.asSequence().normalizedAndPadded(state.rangeMin, state.rangeMax)
            }
            .flowOn(dispatchers.io)
            .onCompletion {
                Logger.d(tag = TAG) { "VISUALIZER COMPLETED CLEANING BUFFER" }
            }

    private fun ReadOnlyShortBuffer.rms(): Float {
        if (isEmpty) return 0f
        var sum = 0.0
        for (i in 0 until size) {
            val sample = this[i].toDouble()
            sum += sample * sample
        }
        return sqrt(sum / size).toFloat()
    }

    private fun Sequence<RecorderPoint>.normalizedAndPadded(min: Int, max: Int) =
        smoothen(factor = .3f)
            .normalize(max = max, min = min)
            .padListWithExtra(bufferSize * 2)
            .toProperSequence(bufferSize)
            .distinctBy { it.timeInMillis }
}
