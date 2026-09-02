package com.sam.talkdraft.recorder.domain

import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.recorder.domain.utils.ReadOnlyShortBuffer
import kotlinx.coroutines.flow.Flow

internal interface IAudioPCMReader {

    suspend fun initReader()
    suspend fun start()
    suspend fun stop()
    fun releaseReader()

    fun readRecorderRawBytes(state: RecorderState): Flow<ReadOnlyShortBuffer>
}
