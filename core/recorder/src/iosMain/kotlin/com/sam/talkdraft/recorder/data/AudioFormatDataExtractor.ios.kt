package com.sam.talkdraft.recorder.data

import com.sam.talkdraft.recorder.domain.IAudioFormatDataExtractor
import com.sam.talkdraft.recorder.domain.models.RecordingFormats
import org.koin.core.annotation.Factory
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.UniformTypeIdentifiers.UTTypeMPEG4Audio

@Factory(binds = [IAudioFormatDataExtractor::class])
internal actual class AudioFormatDataExtractor : IAudioFormatDataExtractor {
    actual override fun getEncoder(format: RecordingFormats): Int {
        return when (format) {
            RecordingFormats.FORMAT_M4A -> kAudioFormatMPEG4AAC.toInt()
        }
    }

    actual override fun getMimeType(format: RecordingFormats): String {
        return when (format) {
            RecordingFormats.FORMAT_M4A -> UTTypeMPEG4Audio.preferredMIMEType ?: "audio/mp4"
        }
    }

    actual override fun getFileExtension(format: RecordingFormats): String {
        TODO("Not yet implemented")
    }

    actual override fun getOutputFormat(format: RecordingFormats): Int {
        return when (format) {
            RecordingFormats.FORMAT_M4A -> kAudioFormatMPEG4AAC.toInt()
        }
    }

}
