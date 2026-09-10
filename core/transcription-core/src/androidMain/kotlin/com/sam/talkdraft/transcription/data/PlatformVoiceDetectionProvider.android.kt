package com.sam.talkdraft.transcription.data

import android.content.Context
import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.transcription.domain.IVoiceDetectionProvider
import com.sam.talkdraft.transcription.domain.model.VoiceDetectionResult
import com.sam.talkdraft.transcription_android.NativeVoiceActivityDetector
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

private const val TAG = "AndroidVoiceDetector"

@Factory(binds = [IVoiceDetectionProvider::class])
internal actual class PlatformVoiceDetectionProvider(
    private val context: Context,
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IVoiceDetectionProvider {

    private val instance by lazy { NativeVoiceActivityDetector() }

    actual override suspend fun setup(sampleRate: Int) {
        val modelName = NativeVoiceActivityDetector.MODEL_NAME
        Logger.d(tag = TAG) { "SETTING UP VOICE RECORDER WITH ASSETS WITH MODEL :$modelName" }
        withContext(dispatchers.io) {
            instance.initialize(context.assets, assetName = modelName, sampleRate = sampleRate)
        }
    }

    actual override fun processAudioBuffer(shorts: ShortArray): VoiceDetectionResult {
        val result = instance.processFrame(shorts)
        return VoiceDetectionResult(result.probability, result.isSpeech)
    }

    actual override fun cleanup() {
        Logger.d(tag = TAG) { "CLOSING VOICE DETECTOR" }
        instance.close()
    }
}
