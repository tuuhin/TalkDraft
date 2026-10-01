package com.sam.talkdraft.transcription.data

import com.sam.talkdraft.transcription.domain.ITranscriptionEngine
import com.sam.talkdraft.transcription.domain.model.TranscriberConfig
import com.sam.talkdraft.transcription.domain.model.TranscriptionError
import com.sam.talkdraft.transcription.domain.model.TranscriptionResult
import com.sam.talkdraft.transcription.domain.model.TranscriptionSegmentModel
import com.sam.talkdraft.transcription.ios.IosNativeWhisper
import com.sam.talkdraft.transcription.ios.exception.WhisperFrameFailedException
import com.sam.talkdraft.transcription.ios.models.IosWhisperCodeError
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

@Factory(binds = [ITranscriptionEngine::class])
@Named(value = "whisper_engine")
internal actual class PlatformWhisperTranscriptionEngine : ITranscriptionEngine {

    private val instance by lazy { IosNativeWhisper() }

    actual override suspend fun warmUp(request: TranscriberConfig) {
        val language = request.language ?: "*"
        val success = instance.init(request.modelPath, language)
        if (!success) throw IllegalStateException("Failed to initialize NativeWhisper model at ${request.modelPath}")
    }

    actual override fun processSegment(bytes: ShortArray): TranscriptionResult {
        val processSuccess = try {
            instance.processBytes(bytes, bytes.size)
        } catch (e: WhisperFrameFailedException) {
            val errorCode = e.code
            val error = when (errorCode) {
                IosWhisperCodeError.AudioEmpty -> TranscriptionError.AudioNotFound
                IosWhisperCodeError.Unknown -> TranscriptionError.UnsupportedAudioFormat
                else -> TranscriptionError.TranscriptionFailed
            }
            return TranscriptionResult.Failed(error)
        }
        if (!processSuccess) return TranscriptionResult.Failed(TranscriptionError.AudioNotFound)
        // 3. Extract updated state from native wrapper
        val state = instance.readState()
            ?: return TranscriptionResult.Failed(TranscriptionError.TranscriptionFailed)

        return TranscriptionResult.Success(
            TranscriptionSegmentModel(segmentId = 0L, state.fullText),
        )
    }

    actual override fun reset() {
    }

    actual override fun cleanUp() {
        instance.close()
    }

}
