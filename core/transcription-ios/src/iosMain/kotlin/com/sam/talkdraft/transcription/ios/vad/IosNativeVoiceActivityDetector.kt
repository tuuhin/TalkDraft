package com.sam.talkdraft.transcription.ios.vad

import com.sam.talkdraft.transcription.ios.models.IosVoiceProbability

class IosNativeVoiceActivityDetector : AutoCloseable {

    private val protocol: IosVoiceActivityDetectorProtocol
        get() = IosVoiceActivityDetectorBridge.getProtocol()

    fun initialize(modelPath: String, sampleRate: Int = 16_000, threshold: Float = .5f): Boolean {
        return protocol.initialize(modelPath, sampleRate, threshold)
    }

    fun processFrame(audioFrame: ShortArray): IosVoiceProbability {
        val probability = protocol.processFrame(audioFrame)
        return IosVoiceProbability(probability)
    }

    fun resetState() = protocol.resetState()

    override fun close() = protocol.close()

    val isInitialized: Boolean
        get() = runCatching { protocol }.isSuccess
}
