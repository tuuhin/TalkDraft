package com.sam.talkdraft.recorder.domain

import com.sam.talkdraft.common.model.ReadOnlyShortBuffer
import com.sam.talkdraft.recorder.domain.models.RecorderState
import kotlinx.coroutines.flow.Flow

internal interface IAudioPCMReader {

    suspend fun initReader()
    suspend fun start()
    suspend fun stop()
    fun releaseReader()

    fun readRecorderRawBytes(state: RecorderState): Flow<ReadOnlyShortBuffer>
}
