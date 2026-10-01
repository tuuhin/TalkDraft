package com.sam.talkdraft.transcription.ios

import co.touchlab.kermit.Logger
import com.sam.talkdraft.transcription.ios.models.IosTranscriptionResultSegment
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
            Logger.d(tag = TAG) { "PREPARING ZIP TRANSFORMER" }
            val success = iosProtocol.doInitWithEncoderPath(encoderPath, decoderPath, joinerPath, tokensPath, error.ptr)
            val nsError = error.value
            if (!success) {
                Logger.w(tag = TAG) { "FAILED TO PREPARE ZIP TRANSFORMER FOR PROCESSING" }
                throw IllegalStateException("Failed to initialize ZipFormer instance: ${nsError?.localizedDescription}")
            }
            return@memScoped success
        }
    }

    fun processFrame(audioFrame: ShortArray): IosTranscriptionResultSegment? {
        if (audioFrame.isEmpty()) return null
        val floatAudio = FloatArray(audioFrame.size) { idx -> audioFrame[idx].toFloat() / Short.MAX_VALUE }
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
            if (resultText == null) throw IllegalStateException("Transcription session not configured")

            val segmentId = resultText.segmentId()
            val segment = resultText.segment()
            IosTranscriptionResultSegment(segmentId = segmentId, segment ?: "")
        }
    }

    fun reset() {

    }


    override fun close() {
        Logger.d(tag = TAG) { "CLEARING UP ZIP FORMER INSTANCE" }
        iosProtocol.cleanUp()
    }

    companion object {
        private const val TAG = "IosNativeZipFormer"
    }
}
