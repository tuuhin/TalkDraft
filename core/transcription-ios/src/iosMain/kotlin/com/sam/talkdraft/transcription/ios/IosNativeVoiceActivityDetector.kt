package com.sam.talkdraft.transcription.ios

import co.touchlab.kermit.Logger
import com.sam.talkdraft.transcription.ios.models.IosVADResult
import com.sam.talkdraft.transcription.ios.models.IosVadSegment
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import platform.Foundation.NSError
import swiftPMImport.com.sam.talkdraft.transcription.ios.core.transcription.ios.SherapVADImpl
import swiftPMImport.com.sam.talkdraft.transcription.ios.core.transcription.ios.SherpaVADConfig

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosNativeVoiceActivityDetector {

    private val detector by lazy { SherapVADImpl() }

    private val _segmentChannel = Channel<IosVadSegment>(capacity = 16)
    val segments = _segmentChannel.receiveAsFlow()

    private var sampleRate: Int = 16000

    fun initialize(sampleRate: Int = 16000, threshold: Float = 0.4f): Boolean = memScoped {
        this@IosNativeVoiceActivityDetector.sampleRate = sampleRate
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
        if (!success) {
            Logger.w(tag = TAG) { "FAILED TO INITIALIZE VAD" }
            throw IllegalStateException("Failed to initialize VAD: ${nsError?.localizedDescription}")
        }
        Logger.d(tag = TAG) { "VAD INITIALIZED SUCCESSFULLY" }
        return@memScoped success
    }

    fun processFrame(audioFrame: ShortArray): IosVADResult = memScoped {
        if (audioFrame.isEmpty()) return@memScoped IosVADResult(false)

        val error = alloc<ObjCObjectVar<NSError?>>()
        val floatAudio = FloatArray(audioFrame.size) { idx -> audioFrame[idx].toFloat() / Short.MAX_VALUE }
        val featureList = floatAudio.toList()

        val isSpeech = detector.acceptWithSamples(featureList)
        val nsError = error.value
        if (nsError != null) throw IllegalStateException("Error during VAD frame processing: ${nsError.localizedDescription}")

        drainNativeSegments(sampleRate)

        return@memScoped IosVADResult(isSpeech)
    }

    private fun drainNativeSegments(rate: Int) {
        while (true) {
            val segmentResult = detector.popSegment() ?: break
            val samples = segmentResult.samples.map { it as Float }.toFloatArray()
            if (samples.isNotEmpty()) {
                val startSampleVal = segmentResult.startSample
                val startDuration = (startSampleVal.toDouble() / rate).toDuration(DurationUnit.SECONDS)
                val endDuration = ((startSampleVal + samples.size).toDouble() / rate).toDuration(DurationUnit.SECONDS)
                val vadSegment = IosVadSegment(
                    startSample = startDuration,
                    endSample = endDuration,
                    samples = samples,
                )
                _segmentChannel.trySend(vadSegment)
            }
        }
    }

    fun flushSpeechSegments() {
        detector.flush()
        drainNativeSegments(sampleRate)
    }

    fun resetState() {
        detector.reset()
        while (true) {
            val result = _segmentChannel.tryReceive()
            if (!result.isSuccess) break
        }
    }

    fun close() {
        Logger.d(tag = TAG) { "CLOSING VAD INSTANCE" }
        detector.close()
        _segmentChannel.close()
    }

    companion object {
        private const val TAG = "IosNativeVoiceActivityDetector"
    }
}
