package com.sam.talkdraft.recorder.domain

import com.sam.talkdraft.recorder.domain.models.RecorderPoint
import kotlinx.coroutines.flow.Flow

interface IAudioVisualizerProvider {

    val dataPoints: Flow<Sequence<RecorderPoint>>
}
