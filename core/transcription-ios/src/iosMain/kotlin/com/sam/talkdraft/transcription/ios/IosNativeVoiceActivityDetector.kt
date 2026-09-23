package com.sam.talkdraft.transcription.ios

import com.sam.talkdraft.transcription.ios.models.IosVoiceProbability
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSError
import swiftPMImport.com.sam.talkdraft.transcription.ios.core.transcription.ios.VoiceActivityDetector

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosNativeVoiceActivityDetector {
    private val detector by lazy { VoiceActivityDetector() }

    val modelPath: String?
        get() = VoiceActivityDetector.getModelPath()

    fun initialize(modelPath: String, sampleRate: Int, threshold: Float): Boolean = memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()

        val success = detector.initializeWithModelPath(
            modelPath = modelPath,
            sampleRate = sampleRate,
            threshold = threshold,
            error = error.ptr,
        )
        val nsError = error.value
        if (!success) throw IllegalStateException("Failed to initialize VAD: ${nsError?.localizedDescription}")

        return@memScoped success
    }

    fun processFrame(audioFrame: FloatArray): IosVoiceProbability = memScoped {

        val error = alloc<ObjCObjectVar<NSError?>>()
        val frameList = audioFrame.toList()
        val probability =
            detector.processFrameWithAudioFrame(audioFrame = frameList, error = error.ptr)

        val nsError = error.value
        if (nsError != null) throw IllegalStateException("Error during VAD frame processing: ${nsError.localizedDescription}")
        return@memScoped IosVoiceProbability(probability)
    }

    fun resetState() = detector.resetState()

    fun close() = detector.close()

}
