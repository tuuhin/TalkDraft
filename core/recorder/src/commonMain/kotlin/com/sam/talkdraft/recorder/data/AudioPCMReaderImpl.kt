package com.sam.talkdraft.recorder.data

import com.sam.talkdraft.recorder.domain.IAudioPCMReader
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.recorder.domain.utils.ReadOnlyShortBuffer
import kotlinx.coroutines.flow.Flow

internal expect class AudioPCMReaderImpl : IAudioPCMReader {
    override suspend fun initReader()
    override fun readRecorderRawBytes(state: RecorderState): Flow<ReadOnlyShortBuffer>
    override fun releaseReader()
    override suspend fun start()
    override suspend fun stop()
}
