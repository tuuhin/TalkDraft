package com.sam.talkdraft.recorder_visualizer.domain

import com.sam.talkdraft.common.model.ReadOnlyFloatBuffer
import kotlinx.coroutines.flow.Flow

fun interface IAudioDynamicVisualizer {

    fun waveformFlow(noOfBlocks: Int): Flow<ReadOnlyFloatBuffer>
}
