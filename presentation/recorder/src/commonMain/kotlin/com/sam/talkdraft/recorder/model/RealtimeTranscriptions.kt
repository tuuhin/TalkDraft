package com.sam.talkdraft.recorder.model

import androidx.compose.runtime.Stable
import com.sam.talkdraft.transcription.domain.model.TranscriptionState
import kotlin.jvm.JvmInline

@Stable
@JvmInline
internal value class RealtimeTranscriptions(val transcriptions: TranscriptionState)
