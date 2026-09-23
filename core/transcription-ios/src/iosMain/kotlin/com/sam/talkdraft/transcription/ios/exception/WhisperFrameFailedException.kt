package com.sam.talkdraft.transcription.ios.exception

import com.sam.talkdraft.transcription.ios.models.IosWhisperCodeError

class WhisperFrameFailedException(val code: IosWhisperCodeError, override val message: String) :
    IllegalStateException(message)

