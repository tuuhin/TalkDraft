package com.sam.talkdraft.transcription.data

import com.sam.talkdraft.transcription.domain.IVoiceDetectionProvider
import com.sam.talkdraft.transcription.domain.model.VoiceDetectionResult
import org.koin.core.annotation.Factory

@Factory(binds = [IVoiceDetectionProvider::class])
expect class PlatformVoiceDetectionProvider : IVoiceDetectionProvider {
    override suspend fun setup(sampleRate: Int)
    override fun processAudioBuffer(shorts: ShortArray): VoiceDetectionResult
    override fun cleanup()
}
