package com.sam.talkdraft.feature_recorder

import com.sam.talkdraft.recorder.domain.models.RecorderPoint
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.transcription.domain.model.TranscriptionState
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ISimpleVoiceRecorder : AutoCloseable {

    val state: StateFlow<RecorderState>
    val elapsedTime: StateFlow<Duration>

    val timeLine: Flow<Sequence<RecorderPoint>>
    val transcription: Flow<TranscriptionState>

    val errors: Flow<Exception>

    suspend fun setup()

    suspend fun start()
    suspend fun stop()
    suspend fun pause()
    suspend fun resume()
    suspend fun cancel()
}
