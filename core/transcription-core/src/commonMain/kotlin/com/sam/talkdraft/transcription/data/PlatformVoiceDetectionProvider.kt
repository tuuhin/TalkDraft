package com.sam.talkdraft.transcription.data

import com.sam.talkdraft.transcription.domain.IVoiceDetectionProvider
import com.sam.talkdraft.transcription.domain.model.TimedVoiceDetectionSegment
import com.sam.talkdraft.transcription.domain.model.VoiceDetectionResult
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory(binds = [IVoiceDetectionProvider::class])
internal expect class PlatformVoiceDetectionProvider : IVoiceDetectionProvider {
    override val speechSegments: Flow<TimedVoiceDetectionSegment>
    override suspend fun setup(sampleRate: Int, silenceThreshold: Float): Boolean
    override suspend fun processAudioBuffer(shorts: ShortArray): VoiceDetectionResult
    override suspend fun reset()
    override suspend fun flushSegments()
    override fun cleanup()
}
