package com.sam.talkdraft.transcription.domain

import com.sam.talkdraft.transcription.domain.model.TranscriberConfig
import com.sam.talkdraft.transcription.domain.model.TranscriptionState

internal interface ITranscriptionEngine {

    suspend fun warmUp(request: TranscriberConfig)
    fun process(bytes: ShortArray): TranscriptionState
    fun cleanUp()
}
