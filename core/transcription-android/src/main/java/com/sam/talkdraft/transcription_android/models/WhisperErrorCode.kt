package com.sam.talkdraft.transcription_android.models

enum class WhisperErrorCode(val code: Int) {
    NONE(0),
    MODEL_LOAD_FAILED(1),
    INFERENCE_FAILED(2),
    INVALID_BUFFER(3),
    BUFFER_FULL(4),
    UNKNOWN(-1);

    companion object {
        fun fromCode(code: Int): WhisperErrorCode {
            return entries.find { it.code == code } ?: UNKNOWN
        }
    }
}
