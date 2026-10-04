package com.sam.talkdraft.transcription.data

import android.content.Context
import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.model.ReadOnlyFloatBuffer
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.transcription.domain.IVoiceDetectionProvider
import com.sam.talkdraft.transcription.domain.model.TimedVoiceDetectionSegment
import com.sam.talkdraft.transcription.domain.model.VoiceDetectionResult
import com.sam.talkdraft.transcription_android.NativeVoiceActivityDetector
import com.sam.talkdraft.transcription_android.vad.AndroidVadConfig
import com.sam.talkdraft.transcription_android.vad.AndroidVadSegment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

private const val TAG = "AndroidVoiceDetector"

@Factory(binds = [IVoiceDetectionProvider::class])
internal actual class PlatformVoiceDetectionProvider(
    private val context: Context,
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IVoiceDetectionProvider {

    private val instance by lazy { NativeVoiceActivityDetector(context) }
    private val _isInstanceReady = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    actual override val speechSegments: Flow<TimedVoiceDetectionSegment>
        get() = _isInstanceReady.flatMapLatest { isReady ->
            // keep the flow running but without any values
            if (!isReady) flow { awaitCancellation() }
            // segments out
            else instance.segments.map { segment: AndroidVadSegment ->
                val sample = ReadOnlyFloatBuffer.wrap(segment.samples, segment.samples.size)
                TimedVoiceDetectionSegment(samples = sample, timedDuration = segment.duration)
            }
        }

    actual override suspend fun setup(sampleRate: Int, silenceThreshold: Float): Boolean {
        Logger.d(tag = TAG) { "SETTING UP VOICE RECORDER SAMPLE RATE:$sampleRate SILENCE_THRESHOLD:$silenceThreshold " }
        return withContext(dispatchers.io) {
            val success = instance.initialize(
                assets = context.assets,
                config = AndroidVadConfig(
                    sampleRate = sampleRate,
                    silenceThreshold = silenceThreshold,
                    minSilenceInSeconds = .05f,
                    minSpeechInSeconds = 0.05f,
                ),
            )
            Logger.d(tag = TAG) { "VAD SETUP COMPLETED SUCCESSFULLY?: $success" }
            _isInstanceReady.updateAndGet { success }
        }
    }

    actual override suspend fun processAudioBuffer(shorts: ShortArray): VoiceDetectionResult {
        return withContext(dispatchers.io) {
            val result = instance.processFrame(shorts)
            VoiceDetectionResult(result.isSpeech)
        }
    }

    actual override suspend fun reset() = withContext(dispatchers.io) {
        Logger.i(tag = TAG) { "RESET ON VAD INSTANCE" }
        instance.resetState()
    }

    actual override suspend fun flushSegments() = withContext(dispatchers.io) {
        Logger.d(tag = TAG) { "FLUSHING OUT SPEECH SEGMENTS" }
        instance.flushSpeechSegments()
    }

    actual override fun cleanup() {
        Logger.d(tag = TAG) { "CLOSING VOICE DETECTOR" }
        _isInstanceReady.value = false
        instance.close()
    }
}
