package com.sam.talkdraft.recorder.data

import com.sam.talkdraft.recorder.domain.IAudioFormatDataExtractor
import com.sam.talkdraft.recorder.domain.models.RecordingFormats
import org.koin.core.annotation.Factory

@Factory(binds = [IAudioFormatDataExtractor::class])
internal expect class AudioFormatDataExtractor : IAudioFormatDataExtractor {
    override fun getEncoder(format: RecordingFormats): Int
    override fun getMimeType(format: RecordingFormats): String
    override fun getOutputFormat(format: RecordingFormats): Int
    override fun getFileExtension(format: RecordingFormats): String
}
