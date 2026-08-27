package com.sam.talkdraft.recorder.domain

import com.sam.talkdraft.recorder.domain.models.RecorderState
import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow
import okio.Path

interface IVoiceRecorder {

    val state: StateFlow<RecorderState>
    val elapsedTime: StateFlow<Duration>
    suspend fun start()
    suspend fun stop(): Result<Path>
    suspend fun pause()
    suspend fun resume()
    suspend fun cancel()
    fun release()
}
