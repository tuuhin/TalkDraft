package com.sam.talkdraft.transcription.domain

import com.sam.talkdraft.common.model.ReadOnlyFloatBuffer
import com.sam.talkdraft.transcription.domain.model.TimedVoiceDetectionSegment
import com.sam.talkdraft.transcription.domain.model.VoiceDetectionResult
import kotlinx.coroutines.flow.Flow

/**
 * Provider contract for real-time Voice Activity Detection (VAD) and audio segment extraction.
 *
 * ### Typical Lifecycle
 * 1. Invoke [setup] to initialize the underlying native resources and configuration.
 * 2. Collect [speechSegments] on a coroutine scope to consume extracted audio segments.
 * 3. Continually call [processAudioBuffer] with incoming short-array PCM chunks.
 * 4. Call [reset] or [flushSegments] when clearing intermediate pipeline states.
 * 5. Call [cleanup] when tearing down the provider instance.
 */
internal interface IVoiceDetectionProvider {

    /**
     * A cold [Flow] that emits detected speech segments wrapped as [ReadOnlyFloatBuffer].
     *
     * Subscribing to this flow automatically triggers [flushSegments] upon collection start to clear
     * stale audio buffers. Emitted buffers contain normalized float PCM audio samples corresponding
     * strictly to detected speech windows.
     */
    val speechSegments: Flow<TimedVoiceDetectionSegment>

    /**
     * Initializes the underlying VAD engine with the required sample rate and threshold settings.
     *
     * Must be called prior to calling [processAudioBuffer] or subscribing to [speechSegments].
     *
     * @param sampleRate Target audio sampling frequency in Hz (typically `16000` or `8000`).
     * @param silenceThreshold Sensitivity threshold for voice detection between `0.0f` and `1.0f`.
     * Higher values require stronger confidence before speech is registered.
     * @return `true` if initialization succeeded, `false` if native resource setup failed.
     */
    suspend fun setup(sampleRate: Int = 16_000, silenceThreshold: Float = .4f): Boolean

    /**
     * Processes an incoming chunk of raw PCM 16-bit audio samples (`ShortArray`).
     *
     * Audio frames are buffered internally into fixed-size windows for VAD inference.
     *
     * @param shorts Raw PCM 16-bit mono audio samples to evaluate.
     * @return [VoiceDetectionResult] indicating whether speech was detected within the processed frames.
     * @throws IllegalStateException If called before [setup] or if internal sample buffers overflow.
     */
    suspend fun processAudioBuffer(shorts: ShortArray): VoiceDetectionResult

    /**
     * Resets internal RNN model states and clears any pending un-processed PCM samples in the carry-over buffer.
     *
     * Call this when an audio session restarts or when transitioning between different speakers/streams
     * without unallocating underlying native handles.
     */
    suspend fun reset()

    /**
     * Flushes intermediate native speech buffers and resets internal sample counters.
     *
     * Useful for forcing pending audio segments out of the queue or discarding partial segments
     * before initiating new audio consumption.
     */
    suspend fun flushSegments()

    /**
     * Releases underlying native resources, model allocations, and invalidates native memory handles.
     *
     * Once cleaned up, the instance must be re-initialized via [setup] before further usage.
     */
    fun cleanup()
}
