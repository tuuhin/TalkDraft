package com.sam.talkdraft.transcription_android

import com.sam.talkdraft.transcription_android.models.WhisperErrorCode
import com.sam.talkdraft.transcription_android.models.WhisperState

class NativeWhisper {

    private var nativePtr: Long = 0L

    external fun init(modelPath: String, language: String): Boolean
    external fun processBytes(samples: ShortArray, length: Int): Boolean
    external fun readState(): WhisperState?
    external fun readError(): WhisperErrorCode?
    external fun close()

    companion object {
        init {
            System.loadLibrary("native_transcriptions")
        }
    }

}
