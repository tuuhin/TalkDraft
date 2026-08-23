package com.sam.talkdraft.transcription.data

import com.sam.talkdraft.transcription.domain.ITranscriptionEngine
import com.sam.talkdraft.transcription.domain.model.TranscriptionError
import com.sam.talkdraft.transcription.domain.model.TranscriptionRequestMetadata
import com.sam.talkdraft.transcription.domain.model.TranscriptionResultModel
import com.sam.talkdraft.transcription.domain.model.TranscriptionSegmentModel
import com.sam.talkdraft.transcription.domain.model.TranscriptionState
import com.sam.talkdraft.transcription.ios.IosWhisperBridge
import com.sam.talkdraft.transcription.ios.models.IosBridgeWhisperCodeError
import org.koin.core.annotation.Factory

@Factory(binds = [ITranscriptionEngine::class])
internal actual class PlatformTranscriptionEngine : ITranscriptionEngine {
    private val instance by lazy { IosWhisperBridge.getProtocol() }

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
        if (errorCode != null) return TranscriptionState.Failed(errorCode.toDomainError())


        // 3. Extract updated state from native wrapper
        val state = instance.readState()
            ?: return TranscriptionState.Failed(TranscriptionError.TranscriptionFailed)

        return TranscriptionState.Completed(
            result = TranscriptionResultModel(
                text = state.fullText,
                segments = state.segment.map { TranscriptionSegmentModel(text = it.text) } ?: emptyList(),
            ),
        )
    }

    actual override fun cleanUp() {
        instance.close()
    }

    private fun IosBridgeWhisperCodeError.toDomainError(): TranscriptionError = when (this) {
        IosBridgeWhisperCodeError.AudioEmpty -> TranscriptionError.AudioNotFound
        IosBridgeWhisperCodeError.AudioProcessing -> TranscriptionError.TranscriptionFailed
        IosBridgeWhisperCodeError.ModelLoadFailed -> TranscriptionError.UnsupportedAudioFormat
        IosBridgeWhisperCodeError.TranscriptionFailed -> TranscriptionError.TranscriptionFailed
    }
}
