package com.sam.talkdraft.recorder.composable

import androidx.compose.runtime.Composable
import com.sam.talkdraft.transcription.domain.model.TranscriptionError

val TranscriptionError.uiMessage: String
    @Composable
    get() = when (this) {
        TranscriptionError.AudioNotFound -> "Unable to read audio"
        TranscriptionError.UnsupportedAudioFormat -> "Unsupported audio format"
        TranscriptionError.TranscriptionFailed -> "Failed Transcription"
    }
