package com.sam.talkdraft.recorder.domain.stopwatch

import co.touchlab.kermit.Logger
import com.sam.talkdraft.recorder.RecorderConstants
import com.sam.talkdraft.recorder.domain.models.RecorderState
import kotlin.time.Clock
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive

private const val TAG = "RecorderStopWatch"

@OptIn(ExperimentalCoroutinesApi::class)
internal class RecorderStopWatch(
    scope: CoroutineScope,
    private val delayTime: Duration = RecorderConstants.STOPWATCH_DELAY_RATE,
) {
    private val _state = MutableStateFlow(RecorderState.IDLE)
    val recorderState: StateFlow<RecorderState> = _state.asStateFlow()

    private val _elapsedTime = MutableStateFlow(Duration.ZERO)
    val elapsedTime: StateFlow<Duration> = _elapsedTime.asStateFlow()

    init {
        _state
            .flatMapLatest { state -> runStopWatch(isRunning = state == RecorderState.RECORDING) }
            .onEach { diff -> _elapsedTime.update { prev -> prev + diff } }
            .catch { err -> Logger.e(tag = TAG) { "Stopwatch error: ${err.message}" } }
            .launchIn(scope)
    }

    private fun runStopWatch(isRunning: Boolean): Flow<Duration> = flow {
        if (!isRunning) return@flow
        var lastMark = Clock.System.now()

        while (currentCoroutineContext().isActive) {
            delay(delayTime)
            val now = Clock.System.now()
            val diff = now - lastMark
            lastMark = now
            emit(diff)
        }
    }

    fun startOrResume() = _state.update { RecorderState.RECORDING }

    fun pause() = _state.update { RecorderState.PAUSED }

    fun prepare() = _state.update { RecorderState.PREPARING }

    fun stop() {
        _state.update { RecorderState.COMPLETED }
    }

    fun cancel() {
        _state.update { RecorderState.CANCELLED }
        _elapsedTime.update { Duration.ZERO }
    }

    fun reset() {
        _state.update { RecorderState.IDLE }
        _elapsedTime.update { Duration.ZERO }
    }
}
