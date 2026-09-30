package com.sam.talkdraft.transcription.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.model.ReadOnlyFloatBuffer
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.transcription.domain.IVoiceDetectionProvider
import com.sam.talkdraft.transcription.domain.model.TimedVoiceDetectionSegment
import com.sam.talkdraft.transcription.domain.model.VoiceDetectionResult
import com.sam.talkdraft.transcription.ios.IosNativeVoiceActivityDetector
import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

private const val TAG = "IOSVoiceDetector"

@OptIn(BetaInteropApi::class)
@Factory(binds = [IVoiceDetectionProvider::class])
internal actual class PlatformVoiceDetectionProvider(
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IVoiceDetectionProvider {

    private val protocol by lazy { IosNativeVoiceActivityDetector() }
    private val _isInstanceReady = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    actual override val speechSegments: Flow<TimedVoiceDetectionSegment>
        get() = _isInstanceReady.flatMapLatest { isReady ->
            if (!isReady) emptyFlow()
            else protocol.segments.map { segment ->
                val sample = ReadOnlyFloatBuffer.wrap(segment.samples, segment.samples.size)
                TimedVoiceDetectionSegment(samples = sample, timedDuration = segment.startSample..segment.endSample)
            }
        }

    actual override suspend fun setup(sampleRate: Int, silenceThreshold: Float): Boolean {
        Logger.d(tag = TAG) { "SETTING UP VOICE RECORDER SAMPLE RATE:$sampleRate SILENCE_THRESHOLD:$silenceThreshold " }
        return withContext(dispatchers.io) {
            val success = protocol.initialize(sampleRate = sampleRate, silenceThreshold)
            Logger.d(tag = TAG) { "VAD SETUP COMPLETED SUCCESSFULLY" }
            _isInstanceReady.updateAndGet { success }
        }
    }

    actual override suspend fun processAudioBuffer(shorts: ShortArray): VoiceDetectionResult {
        return withContext(dispatchers.io) {
            val result = protocol.processFrame(shorts)
            VoiceDetectionResult(result.isSpeech)
        }
    }

    actual override suspend fun reset() = withContext(dispatchers.io) {
        Logger.i(tag = TAG) { "RESET ON VAD INSTANCE" }
        protocol.resetState()
    }

    actual override suspend fun flushSegments() = withContext(dispatchers.io) {
        Logger.d(tag = TAG) { "FLUSHING OUT SPEECH SEGMENTS" }
        protocol.flushSpeechSegments()
    }

    actual override fun cleanup() {
        Logger.d(tag = TAG) { "CLOSING VOICE DETECTOR" }
        _isInstanceReady.value = false
        protocol.close()
    }
}
