package com.sam.talkdraft.recorder.composable

import androidx.compose.runtime.Composable
import com.sam.talkdraft.transcription.domain.model.TranscriptionError
import com.sam.talkdraft.transcription.domain.model.TranscriptionError.AudioNotFound
import com.sam.talkdraft.transcription.domain.model.TranscriptionError.ModelSetupAbsent
import com.sam.talkdraft.transcription.domain.model.TranscriptionError.TranscriptionFailed
import com.sam.talkdraft.transcription.domain.model.TranscriptionError.Unknown
import com.sam.talkdraft.transcription.domain.model.TranscriptionError.UnsupportedAudioFormat

val TranscriptionError.uiMessage: String
    @Composable
    get() = when (this) {
        AudioNotFound -> "Unable to read audio"
        UnsupportedAudioFormat -> "Unsupported audio format"
        TranscriptionFailed -> "Failed Transcription"
        ModelSetupAbsent -> "Model setup missing"
        Unknown -> "Some unknown error"
    }
