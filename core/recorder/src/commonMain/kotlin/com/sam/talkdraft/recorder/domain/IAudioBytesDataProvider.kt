package com.sam.talkdraft.recorder.domain

import com.sam.talkdraft.recorder.domain.utils.ReadOnlyShortBuffer
import kotlinx.coroutines.flow.Flow

internal interface IAudioBytesDataProvider {

    val stream: Flow<ReadOnlyShortBuffer>
}
