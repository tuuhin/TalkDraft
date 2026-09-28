package com.sam.talkdraft.transcription.ios

import com.sam.talkdraft.transcription.ios.exception.WhisperFrameFailedException
import com.sam.talkdraft.transcription.ios.models.IosTranscriptionResultSegment
import com.sam.talkdraft.transcription.ios.models.IosTranscriptionResultState
import com.sam.talkdraft.transcription.ios.models.IosWhisperCodeError
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSError
import swiftPMImport.com.sam.talkdraft.transcription.ios.core.transcription.ios.IosWhisperSegment
import swiftPMImport.com.sam.talkdraft.transcription.ios.core.transcription.ios.SwiftWhisperConnector

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosNativeWhisper {

    private val provider by lazy { SwiftWhisperConnector() }

    fun init(modelPath: String, language: String): Boolean = memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()

        val success = provider.doInitWithModelPath(modelPath, language, error.ptr)
        val nsError = error.value
        if (!success) throw IllegalStateException("Failed to initialize Whisper instance: ${nsError?.localizedDescription}")

        return@memScoped success
    }

    fun processBytes(samples: ShortArray, length: Int): Boolean = memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val frames = samples.toList()
        val success = provider.processBytesWithAudioFrame(frames, length.toLong(), error.ptr)
        val nsError = error.value
        if (!success) {
            val errorCode = error.value?.code ?: throw Exception("Unknown exception while processing bytes")
            val codeMap = IosWhisperCodeError.fromCode(errorCode.toInt())
            throw WhisperFrameFailedException(
                codeMap,
                "Failed to work with the given frame: ${nsError?.localizedDescription}",
            )
        }

        return@memScoped success

    }

    fun readState(): IosTranscriptionResultState? {
        val state = provider.readState() ?: return null
        val segments = state.segment.filterIsInstance<IosWhisperSegment>()
            .map { IosTranscriptionResultSegment(0L, it.text, startTimeMs = it.startTimeMs, endTimeMs = it.endTimeMs) }
        return IosTranscriptionResultState(state.fullText, segments)
    }

    fun close() = provider.close()
}
