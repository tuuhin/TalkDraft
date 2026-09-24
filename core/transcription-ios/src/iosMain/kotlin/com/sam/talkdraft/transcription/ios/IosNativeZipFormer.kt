package com.sam.talkdraft.transcription.ios

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSError
import swiftPMImport.com.sam.talkdraft.transcription.ios.core.transcription.ios.IosZipFormer

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosNativeZipFormer : AutoCloseable {

    private val iosProtocol by lazy { IosZipFormer() }

    fun initialize(encoderPath: String, decoderPath: String, joinerPath: String, tokensPath: String): Boolean {
        return memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            val success = iosProtocol.doInitWithEncoderPath(encoderPath, decoderPath, joinerPath, tokensPath, error.ptr)
            val nsError = error.value
            if (!success) throw IllegalStateException("Failed to initialize ZipFormer instance: ${nsError?.localizedDescription}")
            return@memScoped success
        }
    }

    fun processFrame(audioFrame: ShortArray): String? {
        if (audioFrame.isEmpty()) return null
        val floatAudio = FloatArray(audioFrame.size) { i ->
            audioFrame[i] / 32768.0f
        }

        return memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            val featureArray = floatAudio.toList()

            // 3. Pin the FloatArray memory to safely pass its raw pointer to Objective-C / Swift
            val resultText = iosProtocol.transcribe(
                melFeatures = featureArray,
                numFrames = floatAudio.size.toLong(),
                error = error.ptr,
            )
            val nsError = error.value
            if (nsError != null) throw IllegalStateException("Transcription failed: ${nsError.localizedDescription}")
            resultText
        }
    }

    override fun close() = iosProtocol.cleanUp()
}
