package com.sam.talkdraft.transcription.data

import com.sam.talkdraft.transcription.domain.ITranscriptionEngine
import com.sam.talkdraft.transcription.domain.model.TranscriberConfig
import com.sam.talkdraft.transcription.domain.model.TranscriptionResult
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

@Factory(binds = [ITranscriptionEngine::class])
@Named(value = "zip_former_engine")
internal expect class PlatformZipFormerTranscriptionEngine : ITranscriptionEngine {
    override suspend fun warmUp(request: TranscriberConfig)
    override fun processSegment(bytes: ShortArray): TranscriptionResult
    override fun reset()
    override fun cleanUp()
}
