package com.sam.talkdraft.transcription.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.transcription.domain.IVoiceDetectionProvider
import com.sam.talkdraft.transcription.domain.model.VoiceDetectionResult
import com.sam.talkdraft.transcription.ios.IosNativeVoiceActivityDetector
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

private const val TAG = "IOSVoiceDetector"

@OptIn(BetaInteropApi::class)
@Factory(binds = [IVoiceDetectionProvider::class])
internal actual class PlatformVoiceDetectionProvider(
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IVoiceDetectionProvider {

    private val protocol by lazy { IosNativeVoiceActivityDetector() }

    actual override suspend fun setup(sampleRate: Int) {

        val path = protocol.modelPath
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
        if (result.probability > .7f) Logger.d(tag = TAG) { "VOICE_PROBABILITY :${result}" }
        return VoiceDetectionResult(result.probability, result.probability > .5f)
    }

    actual override fun cleanup() {
        Logger.d(tag = TAG) { "CLOSING VOICE DETECTOR" }
        protocol.close()
    }
}
