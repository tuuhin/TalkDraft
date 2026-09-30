package com.sam.talkdraft.transcription.data

import com.sam.talkdraft.common.model.ReadOnlyFloatBuffer
import com.sam.talkdraft.transcription.domain.IVoiceDetectionProvider
import com.sam.talkdraft.transcription.domain.model.VoiceDetectionResult
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory(binds = [IVoiceDetectionProvider::class])
internal expect class PlatformVoiceDetectionProvider : IVoiceDetectionProvider {
    override val speechSegments: Flow<ReadOnlyFloatBuffer>
    override suspend fun setup(sampleRate: Int, silenceThreshold: Float): Boolean
    override fun processAudioBuffer(shorts: ShortArray): VoiceDetectionResult
    override suspend fun reset()
    override suspend fun flushSegments()
    override fun cleanup()
}
