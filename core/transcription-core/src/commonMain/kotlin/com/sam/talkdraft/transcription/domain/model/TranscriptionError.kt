package com.sam.talkdraft.transcription.domain.model

enum class TranscriptionError {
    AudioNotFound,
    ModelSetupAbsent,
    UnsupportedAudioFormat,
    TranscriptionFailed,
    Unknown
}
