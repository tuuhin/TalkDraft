package com.sam.talkdraft.recorder.data

import com.sam.talkdraft.recorder.domain.IAudioPCMReader
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.recorder.domain.utils.ReadOnlyShortBuffer
import kotlinx.coroutines.flow.Flow

internal expect class AudioPCMReader : IAudioPCMReader {
    override fun initReader()
    override fun readRecorderRawBytes(state: RecorderState): Flow<ReadOnlyShortBuffer>
    override fun releaseReader()
    override fun start()
    override fun stop()
}
