package com.sam.talkdraft.transcription.ios.models

sealed class IosBridgeWhisperCodeError {
    data object ModelLoadFailed : IosBridgeWhisperCodeError()
    data object AudioEmpty : IosBridgeWhisperCodeError()
    data object TranscriptionFailed : IosBridgeWhisperCodeError()
    data object AudioProcessing : IosBridgeWhisperCodeError()
}
