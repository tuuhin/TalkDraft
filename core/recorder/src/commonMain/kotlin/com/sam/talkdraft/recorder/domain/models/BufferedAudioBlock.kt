package com.sam.talkdraft.recorder.domain.models

data class BufferedAudioBlock(
    val timeInMillis: Long = 0L,
    val rmsValue: Float = 0f,
    val isPaddingPoint: Boolean = false,
)
