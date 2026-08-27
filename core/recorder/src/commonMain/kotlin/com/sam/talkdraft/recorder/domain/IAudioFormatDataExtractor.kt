package com.sam.talkdraft.recorder.domain

import com.sam.talkdraft.recorder.domain.models.RecordingFormats

internal interface IAudioFormatDataExtractor {
    fun getEncoder(format: RecordingFormats): Int
    fun getOutputFormat(format: RecordingFormats): Int
    fun getMimeType(format: RecordingFormats): String
    fun getFileExtension(format: RecordingFormats): String
}
