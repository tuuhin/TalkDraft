package com.sam.talkdraft.transcription.domain

import com.sam.talkdraft.common.model.ReadOnlyShortBuffer
import com.sam.talkdraft.transcription.domain.model.TranscriberConfig
import com.sam.talkdraft.transcription.domain.model.TranscriberEngine
import com.sam.talkdraft.transcription.domain.model.TranscriptionResult
import kotlinx.coroutines.flow.Flow

interface ITranscriberResultsProvider {

    fun transcribe(audioFrame: Flow<ReadOnlyShortBuffer>): Flow<TranscriptionResult>

    suspend fun setConfig(
        config: TranscriberConfig,
        engine: TranscriberEngine = TranscriberEngine.WHISPER,
    ): Result<Boolean>

    fun clearConfig()

    suspend fun reset()
}
