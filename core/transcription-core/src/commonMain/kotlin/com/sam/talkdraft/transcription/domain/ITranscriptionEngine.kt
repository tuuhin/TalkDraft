package com.sam.talkdraft.transcription.domain

import com.sam.talkdraft.transcription.domain.model.TranscriptionRequestMetadata
import com.sam.talkdraft.transcription.domain.model.TranscriptionState

interface ITranscriptionEngine {

    suspend fun warmUp(request: TranscriptionRequestMetadata)
    fun process(bytes: ShortArray): TranscriptionState
    fun cleanUp()
}
