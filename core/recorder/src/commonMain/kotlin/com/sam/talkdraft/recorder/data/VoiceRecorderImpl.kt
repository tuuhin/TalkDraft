package com.sam.talkdraft.recorder.data

import com.sam.talkdraft.recorder.domain.IAudioBytesDataProvider
import com.sam.talkdraft.recorder.domain.IVoiceRecorder
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.recorder.domain.utils.ReadOnlyShortBuffer
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import okio.Path

internal expect class VoiceRecorderImpl : IVoiceRecorder, IAudioBytesDataProvider {
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
