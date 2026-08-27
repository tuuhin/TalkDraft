package com.sam.talkdraft.recorder.data

import android.media.MediaRecorder
import androidx.media3.common.MimeTypes
import com.sam.talkdraft.recorder.domain.IAudioFormatDataExtractor
import com.sam.talkdraft.recorder.domain.models.RecordingFormats
import org.koin.core.annotation.Factory

@Factory(binds = [IAudioFormatDataExtractor::class])
internal actual class AudioFormatDataExtractor : IAudioFormatDataExtractor {
    actual override fun getEncoder(format: RecordingFormats): Int {
        return when (format) {
            RecordingFormats.FORMAT_M4A -> MediaRecorder.AudioEncoder.AAC
        }
    }

    actual override fun getMimeType(format: RecordingFormats): String {
        return when (format) {
            RecordingFormats.FORMAT_M4A -> MimeTypes.AUDIO_MP4
        }
    }

    actual override fun getOutputFormat(format: RecordingFormats): Int {
        return when (format) {
            RecordingFormats.FORMAT_M4A -> MediaRecorder.OutputFormat.MPEG_4
        }
    }
}
