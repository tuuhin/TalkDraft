package com.sam.talkdraft.transcription.domain

import com.sam.talkdraft.transcription.domain.model.VoiceDetectionResult

internal interface IVoiceDetectionProvider {

    suspend fun setup(sampleRate: Int = 16_000)

    fun processAudioBuffer(shorts: ShortArray): VoiceDetectionResult

    fun cleanup()
}
