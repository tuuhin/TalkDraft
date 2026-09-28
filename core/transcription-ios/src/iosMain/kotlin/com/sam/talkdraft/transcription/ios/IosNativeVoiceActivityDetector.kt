package com.sam.talkdraft.transcription.ios

import com.sam.talkdraft.transcription.ios.models.IosVADResult
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSError
import swiftPMImport.com.sam.talkdraft.transcription.ios.core.transcription.ios.SherapVADImpl
import swiftPMImport.com.sam.talkdraft.transcription.ios.core.transcription.ios.SherpaVADConfig

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosNativeVoiceActivityDetector {

    private val detector by lazy { SherapVADImpl() }

    fun initialize(sampleRate: Int, threshold: Float): Boolean = memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val config = SherpaVADConfig().apply {
            this.threshold = threshold
            this.sampleRate = sampleRate
        }
        val success = detector.initializeWithConfig(
            config = config,
            error = error.ptr,
        )
        val nsError = error.value
        if (!success) throw IllegalStateException("Failed to initialize VAD: ${nsError?.localizedDescription}")

        return@memScoped success
    }

    fun processFrame(audioFrame: FloatArray): IosVADResult = memScoped {

        val error = alloc<ObjCObjectVar<NSError?>>()
        val frameList = audioFrame.toList()
        val probability = detector.acceptWithSamples(frameList)

        val nsError = error.value
        if (nsError != null) throw IllegalStateException("Error during VAD frame processing: ${nsError.localizedDescription}")
        return@memScoped IosVADResult(probability)
    }

    fun resetState() = detector.reset()

    fun close() = detector.close()

}
