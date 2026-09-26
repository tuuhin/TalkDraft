package com.sam.talkdraft.transcription.data

import com.sam.talkdraft.transcription.domain.ITranscriptionEngine
import com.sam.talkdraft.transcription.domain.model.TranscriberConfig
import com.sam.talkdraft.transcription.domain.model.TranscriptionState
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

@Factory(binds = [ITranscriptionEngine::class])
@Named(value = "whisper_engine")
internal expect class PlatformWhisperTranscriptionEngine : ITranscriptionEngine {
    override suspend fun warmUp(request: TranscriberConfig)
    override fun process(bytes: ShortArray): TranscriptionState
    override fun reset()
    override fun cleanUp()
}
