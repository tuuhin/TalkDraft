package com.sam.talkdraft.transcription.data

import com.sam.talkdraft.transcription.domain.ITranscriptionEngine
import com.sam.talkdraft.transcription.domain.model.TranscriptionRequestMetadata
import com.sam.talkdraft.transcription.domain.model.TranscriptionState
import org.koin.core.annotation.Factory

@Factory(binds = [ITranscriptionEngine::class])
internal expect class PlatformTranscriptionEngine : ITranscriptionEngine {
    override fun warmUp(request: TranscriptionRequestMetadata)
    override fun process(bytes: ShortArray): TranscriptionState
    override fun cleanUp(): Unit
}
