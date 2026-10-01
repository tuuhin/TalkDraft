package com.sam.talkdraft.transcription.domain

import com.sam.talkdraft.transcription.domain.model.TranscriberConfig
import com.sam.talkdraft.transcription.domain.model.TranscriptionResult

internal interface ITranscriptionEngine {

    suspend fun warmUp(request: TranscriberConfig)
    fun processSegment(bytes: ShortArray): TranscriptionResult
    fun reset()
    fun cleanUp()
}
