package com.sam.talkdraft.transcription.data

import com.sam.talkdraft.transcription.domain.ITranscriptionEngine
import com.sam.talkdraft.transcription.domain.model.TranscriberConfig
import com.sam.talkdraft.transcription.domain.model.TranscriptionError
import com.sam.talkdraft.transcription.domain.model.TranscriptionSegmentModel
import com.sam.talkdraft.transcription.domain.model.TranscriptionState
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

    actual override fun process(bytes: ShortArray): TranscriptionState {
        val processSuccess = try {
            instance.processBytes(bytes, bytes.size)
        } catch (e: WhisperFrameFailedException) {
            val errorCode = e.code
            val error = when (errorCode) {
                IosWhisperCodeError.AudioEmpty -> TranscriptionError.AudioNotFound
                IosWhisperCodeError.Unknown -> TranscriptionError.UnsupportedAudioFormat
                else -> TranscriptionError.TranscriptionFailed
            }
            return TranscriptionState.Failed(error)
        }
        if (!processSuccess) return TranscriptionState.Failed(TranscriptionError.AudioNotFound)
        // 3. Extract updated state from native wrapper
        val state = instance.readState()
            ?: return TranscriptionState.Failed(TranscriptionError.TranscriptionFailed)

        return TranscriptionState.Success(
            text = state.fullText,
            segments = state.segment.mapIndexed { index, it ->
                TranscriptionSegmentModel(
                    segmentId = index.toLong(),
                    text = it.text,
                )
            },
        )
    }

    actual override fun reset() {
    }

    actual override fun cleanUp() {
        instance.close()
    }

}
