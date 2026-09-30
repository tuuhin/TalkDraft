package com.sam.talkdraft.transcription_android.vad

/**
 * Configuration for the Android Voice Activity Detector.
 *
 * @property sampleRate Audio sample rate in Hz. Supported values are 8,000 and 16,000.
 * @property silenceThreshold Probability threshold used by the VAD to distinguish
 * speech from silence. Higher values generally require stronger speech confidence.
 * @property minSilenceInSeconds Minimum duration of silence, in seconds, required
 * to finalize a detected speech segment.
 * @property minSpeechInSeconds Minimum duration of speech, in seconds, required
 * for a speech segment to be considered valid.
 * @property maxSpeechInSeconds Maximum duration of a single speech segment, in seconds.
 * Longer continuous speech may be split into multiple segments.
 * @property noOfThreads Number of threads used by the underlying VAD inference engine.
 * @property bufferSeconds Size of the internal VAD audio buffer, in seconds. This
 * determines the amount of audio the VAD can keep internally; it does not determine
 * how long the VAD waits before emitting a speech segment.
 */
data class AndroidVadConfig(
    val sampleRate: Int = 16_000,
    val silenceThreshold: Float = 0.5f,
    val minSilenceInSeconds: Float = 0.15f,
    val minSpeechInSeconds: Float = 0.05f,
    val maxSpeechInSeconds: Float = 10.0f,
    val noOfThreads: Int = 1,
    val bufferSeconds: Float = 20.0f,
) {
    internal val isValidSampleRate: Boolean
        get() = sampleRate == 16_000 || sampleRate == 8_000
}
