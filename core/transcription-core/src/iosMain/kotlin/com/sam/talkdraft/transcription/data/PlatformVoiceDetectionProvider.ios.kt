package com.sam.talkdraft.transcription.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.transcription.domain.IVoiceDetectionProvider
import com.sam.talkdraft.transcription.domain.model.VoiceDetectionResult
import com.sam.talkdraft.transcription.ios.vad.IosVoiceActivityDetectorBridge
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import platform.Foundation.NSBundle

private const val TAG = "IOSVoiceDetector"

@Factory(binds = [IVoiceDetectionProvider::class])
actual class PlatformVoiceDetectionProvider(
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IVoiceDetectionProvider {

    private val protocol by lazy { IosVoiceActivityDetectorBridge.getProtocol() }

    @OptIn(BetaInteropApi::class)
    actual override suspend fun setup(sampleRate: Int) {

        val path = NSBundle.mainBundle.pathForResource("silero_vad", "onnx")
            ?: throw IllegalStateException("Cannot find the silero file ensure its been added to the main bundle")

        Logger.d(tag = TAG) { "SETTING UP VOICE RECORDER WITH ASSETS WITH MODEL silero_vad" }
        withContext(dispatchers.io) {
            protocol.initialize(path, sampleRate = sampleRate, threshold = .3f)
        }
    }

    actual override fun processAudioBuffer(shorts: ShortArray): VoiceDetectionResult {
        val floatArray = FloatArray(shorts.size)
        for (i in floatArray.indices) {
            val x = shorts[i]
            floatArray[i] = x.toFloat() / Short.MAX_VALUE
        }
        val result = protocol.processFrame(floatArray)
        if (result > .7f) Logger.d(tag = TAG) { "VOICE_PROBABILITY :${result}" }
        return VoiceDetectionResult(result, result > .5f)
    }

    actual override fun cleanup() {
        Logger.d(tag = TAG) { "CLOSING VOICE DETECTOR" }
        protocol.close()
    }
}
