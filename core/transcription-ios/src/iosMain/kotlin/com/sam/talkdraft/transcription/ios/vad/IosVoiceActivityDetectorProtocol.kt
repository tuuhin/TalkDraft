package com.sam.talkdraft.transcription.ios.vad

interface IosVoiceActivityDetectorProtocol {
    fun initialize(modelPath: String, sampleRate: Int, threshold: Float): Boolean
    fun processFrame(audioFrame: ShortArray): Float
    fun resetState()
    fun close()
}
