package com.sam.talkdraft.transcription.ios.models

enum class IosWhisperCodeError(internal val code: Int) {
    ModelLoadFailed(100),
    AudioEmpty(101),
    TranscriptionFailed(102),
    AudioProcessing(103),
    Unknown(-1);

    companion object {
        internal fun fromCode(code: Int): IosWhisperCodeError =
            IosWhisperCodeError.entries.find { it.code == code } ?: Unknown
    }
}
