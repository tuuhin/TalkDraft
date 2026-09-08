package com.sam.talkdraft.transcription.ios.vad

interface IosVoiceActivityDetectorProtocol {
    fun initialize(modelPath: String, sampleRate: Int, threshold: Float): Boolean
    fun processFrame(audioFrame: FloatArray): Float
    fun resetState()
    fun close()
}
