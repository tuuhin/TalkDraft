package com.sam.talkdraft.transcription.domain

import com.sam.talkdraft.transcription.domain.model.TranscriberConfig
import com.sam.talkdraft.transcription.domain.model.TranscriptionEngineOutput

/**
 * Contract for a speech-to-text transcription engine instance capable of processing audio chunks in real time.
 */
internal interface ITranscriptionEngine {

    /**
     * Preloads and prepares the underlying machine learning model or audio resources using the provided configuration.
     * @param request Configuration settings required to initialize the transcription model, such as model path and language.
     * @see TranscriberConfig
     */
    suspend fun warmUp(request: TranscriberConfig)

    /**
     * Processes a single chunk of PCM audio data.
     * @param bytes Raw PCM audio samples represented as 16-bit signed integers ([ShortArray]).
     * @return A [TranscriptionEngineOutput] representing the engine state or result (e.g., transcript segment, buffering, or error).
     */
    fun processSegment(bytes: ShortArray): TranscriptionEngineOutput

    /**
     * Clears internal state buffers and resets context without unloading loaded models or heavy resources.
     */
    fun reset()

    /**
     * Releases all native resources, model memory, and handles associated with this transcription engine.
     */
    fun cleanUp()
}
