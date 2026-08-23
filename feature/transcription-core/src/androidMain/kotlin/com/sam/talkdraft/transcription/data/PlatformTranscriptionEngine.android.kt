package com.sam.talkdraft.transcription.data

import com.sam.talkdraft.transcription.domain.ITranscriptionEngine
import com.sam.talkdraft.transcription.domain.model.TranscriptionError
import com.sam.talkdraft.transcription.domain.model.TranscriptionRequestMetadata
import com.sam.talkdraft.transcription.domain.model.TranscriptionResultModel
import com.sam.talkdraft.transcription.domain.model.TranscriptionSegmentModel
import com.sam.talkdraft.transcription.domain.model.TranscriptionState
import com.sam.talkdraft.transcription_android.NativeWhisper
import com.sam.talkdraft.transcription_android.models.WhisperErrorCode
import org.koin.core.annotation.Factory

@Factory(binds = [ITranscriptionEngine::class])
internal actual class PlatformTranscriptionEngine : ITranscriptionEngine {

    private val instance by lazy { NativeWhisper() }

    actual override fun warmUp(request: TranscriptionRequestMetadata) {
        val language = request.language ?: "*"
        val success = instance.init(request.modelPath, language)
        if (!success) {
            throw IllegalStateException("Failed to initialize NativeWhisper model at ${request.modelPath}")
        }
    }

    actual override fun process(bytes: ShortArray): TranscriptionState {
        val processSuccess = instance.processBytes(bytes, bytes.size)
        if (!processSuccess) {
            val errorCode = instance.readError()
            return TranscriptionState.Failed(errorCode?.toDomainError() ?: TranscriptionError.TranscriptionFailed)
        }

        val errorCode = instance.readError()
        if (errorCode != null && errorCode.code != 0) {
            return TranscriptionState.Failed(errorCode.toDomainError())
        }

        // 3. Extract updated state from native wrapper
        val state = instance.readState()
            ?: return TranscriptionState.Failed(TranscriptionError.TranscriptionFailed)

        return TranscriptionState.Completed(
            result = TranscriptionResultModel(
                text = state.fullText,
                segments = state.segment?.let { listOf(TranscriptionSegmentModel(text = it)) } ?: emptyList(),
            ),
        )
    }

    actual override fun cleanUp() {
        instance.close()
    }

    private fun WhisperErrorCode.toDomainError(): TranscriptionError = when (this.code) {
        101 -> TranscriptionError.UnsupportedAudioFormat
        102 -> TranscriptionError.TranscriptionFailed
        else -> TranscriptionError.TranscriptionFailed
    }
}
