package com.sam.talkdraft.recorder.domain

import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.recorder.domain.utils.ReadOnlyShortBuffer
import kotlinx.coroutines.flow.Flow

internal interface IAudioPCMReader {

    fun initReader()
    fun start()
    fun stop()
    fun releaseReader()

    fun readRecorderRawBytes(state: RecorderState): Flow<ReadOnlyShortBuffer>
}
