package com.sam.talkdraft.recorder_visualizer.domain

import com.sam.talkdraft.recorder.domain.models.BufferedAudioBlock
import kotlinx.coroutines.flow.Flow

interface IAudioBufferedVisualizer {

    val dataPoints: Flow<Sequence<BufferedAudioBlock>>
}
