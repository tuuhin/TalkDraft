package com.sam.talkdraft.transcription.domain

import com.sam.talkdraft.common.model.ReadOnlyShortBuffer
import com.sam.talkdraft.transcription.domain.model.TranscriberConfig
import com.sam.talkdraft.transcription.domain.model.TranscriberEngine
import com.sam.talkdraft.transcription.domain.model.TranscriptionResult
import kotlinx.coroutines.flow.Flow

/**
 * Provides an interface for audio transcription services, handling configuration,
 * state monitoring, and real-time audio frame transcription.
 */
interface ITranscriberResultsProvider {

    /**
     * Emits the operational readiness state of the transcriber engine..
     */
    val isTranscriberReady: Flow<Boolean>

    /**
     * Transcribes an incoming stream of audio frames into transcription results.
     *
     * @param audioFrame A [Flow] emitting raw [ReadOnlyShortBuffer] audio frame chunks.
     * @return A [Flow] emitting incremental or final [TranscriptionResult] instances.
     */
    fun transcribe(audioFrame: Flow<ReadOnlyShortBuffer>): Flow<TranscriptionResult>

    /**
     * Configures the transcription engine with specific parameters.
     *
     * @param config The [TranscriberConfig] settings to apply (e.g., language, model options).
     * @param engine The underlying [TranscriberEngine] implementation to use. Defaults to [TranscriberEngine.WHISPER].
     * @return A [Result] containing `true` if the configuration was applied successfully, or an error payload on failure.
     */
    suspend fun setConfig(
        config: TranscriberConfig,
        engine: TranscriberEngine = TranscriberEngine.WHISPER,
    ): Result<Boolean>

    /**
     * Clears the current transcriber configuration, resetting settings to their default unconfigured state.
     */
    fun clearConfig()

    /**
     * Resets the transcriber engine, releasing transient processing state or internal resources.
     */
    suspend fun reset()
}
