package com.sam.talkdraft.recorder.data

import com.sam.talkdraft.common.model.ReadOnlyShortBuffer
import com.sam.talkdraft.recorder.domain.IVoiceRecorderWithByteReader
import com.sam.talkdraft.recorder.domain.models.RecorderState
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import okio.Path

internal expect class VoiceRecorderImpl : IVoiceRecorderWithByteReader {
    override val state: StateFlow<RecorderState>
    override val elapsedTime: StateFlow<Duration>
    override val stream: Flow<ReadOnlyShortBuffer>
    override suspend fun start()
    override suspend fun resume()
    override suspend fun pause()
    override suspend fun stop(): Result<Path>
    override suspend fun cancel()
    override fun release()
}
