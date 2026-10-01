package com.sam.talkdraft.recorder.domain.models

enum class RecorderState {
    IDLE,
    PREPARING,
    RECORDING,
    PAUSED,
    COMPLETED;

    val isRecording: Boolean
        get() = this == RECORDING

    val canReadAmplitudes: Boolean
        get() = this in arrayOf(RECORDING, PAUSED, PREPARING)
}
