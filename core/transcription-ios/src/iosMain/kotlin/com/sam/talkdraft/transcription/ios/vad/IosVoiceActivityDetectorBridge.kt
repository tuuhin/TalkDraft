package com.sam.talkdraft.transcription.ios.vad

object IosVoiceActivityDetectorBridge {
    private var _protocol: IosVoiceActivityDetectorProtocol? = null

    fun setProtocol(protocol: IosVoiceActivityDetectorProtocol) {
        _protocol = protocol
    }

    fun getProtocol(): IosVoiceActivityDetectorProtocol {
        if (_protocol == null) throw IllegalStateException("IosVadBridge protocol is not initialized! Call IosVadBridge.setProtocol() in Swift during app startup.")
        return _protocol!!
    }
}
