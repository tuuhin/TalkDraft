package com.sam.talkdraft.transcription.domain.model

sealed interface TranscriptionError {

    data object AudioNotFound : TranscriptionError
    data object UnsupportedAudioFormat : TranscriptionError
    data object TranscriptionFailed : TranscriptionError
}
