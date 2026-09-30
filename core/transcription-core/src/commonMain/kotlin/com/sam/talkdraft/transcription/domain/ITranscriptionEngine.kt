package com.sam.talkdraft.transcription.domain

import com.sam.talkdraft.transcription.domain.model.TranscriberConfig
import com.sam.talkdraft.transcription.domain.model.TranscriptionState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal interface ITranscriptionEngine {

    suspend fun warmUp(request: TranscriberConfig)
    fun processSegment(bytes: ShortArray, timeStamp: ClosedRange<Duration> = 0.seconds..0.seconds): TranscriptionState
    fun reset()
    fun cleanUp()
}
