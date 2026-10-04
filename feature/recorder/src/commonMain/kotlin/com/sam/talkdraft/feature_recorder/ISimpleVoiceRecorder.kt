package com.sam.talkdraft.feature_recorder

import com.sam.talkdraft.common.model.ReadOnlyFloatBuffer
import com.sam.talkdraft.recorder.domain.models.RecorderState
import com.sam.talkdraft.transcription.domain.model.TranscriptionResult
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow

/**
 * Manages voice recording sessions, live audio visualizer data, and speech transcription.
 */
interface ISimpleVoiceRecorder : AutoCloseable {

    /** Current state of the recorder (e.g., idle, recording, paused). */
    val recorderState: Flow<RecorderState>

    /** Total duration of the current recording session. */
    val elapsedTime: Flow<Duration>

    /** Real-time amplitude values for drawing waveform graphics. Emits `null` when not recording. */
    val waveform: Flow<ReadOnlyFloatBuffer?>

    /** Current transcription status and output text. */
    val transcriptionResult: Flow<TranscriptionResult>

    /** Stream of non-fatal errors during recording or transcription. */
    val errors: Flow<Exception>

    /** Prepares the mic and resources. Call this before [start]. */
    suspend fun setup(): Result<Unit>

    /** Starts capturing audio. */
    suspend fun start(): Result<Unit>

    /** Stops recording and saves the audio file. */
    suspend fun stop(): Result<Unit>


    suspend fun onSave(): Result<Unit>

    /** Cancels the recording and deletes any temporary files. */
    suspend fun cancel(): Result<Unit>
}
