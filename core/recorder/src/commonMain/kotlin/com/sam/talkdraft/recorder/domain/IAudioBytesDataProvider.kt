package com.sam.talkdraft.recorder.domain

import com.sam.talkdraft.common.model.ReadOnlyShortBuffer
import kotlinx.coroutines.flow.Flow

interface IAudioBytesDataProvider {

    val stream: Flow<ReadOnlyShortBuffer>
}
