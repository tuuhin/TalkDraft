package com.sam.talkdraft.transcription.ios

object IosWhisperBridge {

    private lateinit var _protocol: IosWhisperProtocol

    fun setProtocol(protocol: IosWhisperProtocol) {
        _protocol = protocol
    }

    fun getProtocol(): IosWhisperProtocol {
        check(::_protocol.isInitialized) { "IosWhisperBridge protocol is not set. Call setProtocol() before retrieving." }
        return _protocol
    }
}
