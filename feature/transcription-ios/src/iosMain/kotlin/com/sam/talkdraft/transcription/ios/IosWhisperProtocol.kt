package com.sam.talkdraft.transcription.ios

import com.sam.talkdraft.transcription.ios.models.IosBridgeWhisperCodeError
import com.sam.talkdraft.transcription.ios.models.IosBridgeWhisperState

interface IosWhisperProtocol {

    fun init(modelPath: String, language: String): Boolean
    fun processBytes(samples: ShortArray, length: Int): Boolean
    fun readState(): IosBridgeWhisperState?
    fun readError(): IosBridgeWhisperCodeError?
    fun close()
}
