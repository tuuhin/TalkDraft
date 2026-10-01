@file:OptIn(ExperimentalAtomicApi::class)

package com.sam.talkdraft.transcription.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.transcription.domain.ITranscriptionEngine
import com.sam.talkdraft.transcription.domain.model.TranscriberConfig
import com.sam.talkdraft.transcription.domain.model.TranscriptionError
import com.sam.talkdraft.transcription.domain.model.TranscriptionResult
import com.sam.talkdraft.transcription.domain.model.TranscriptionSegmentModel
import com.sam.talkdraft.transcription_android.NativeWhisper
import com.sam.talkdraft.transcription_android.models.ProcessingState
import com.sam.talkdraft.transcription_android.models.WhisperErrorCode
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

@Factory(binds = [ITranscriptionEngine::class])
@Named(value = "whisper_engine")
internal actual class PlatformWhisperTranscriptionEngine(
    private val dispatcher: IPlatformCoroutineDispatchers,
) : ITranscriptionEngine {

    private val instance by lazy { NativeWhisper() }
    private val _isSetupDone = AtomicBoolean(false)
    private val _lock = Mutex()

    actual override suspend fun warmUp(request: TranscriberConfig) {
        _lock.withLock {
            if (_isSetupDone.load()) {
                Logger.w(tag = TAG) { "WARMUP IS ALREADY COMPLETED" }
                return
            }
            val language = request.language ?: "auto"
            val success = withContext(dispatcher.default) {
                instance.initialize(request.modelPath, "en")
            }
            _isSetupDone.compareAndSet(expectedValue = false, success)
            Logger.d(tag = TAG) { "AUDIO TRANSCRIPTION SETUP COMPLETED :$success" }
        }
    }

    actual override fun processSegment(bytes: ShortArray): TranscriptionResult {

        if (!_isSetupDone.load()) {
            Logger.w(tag = TAG) { "SETUP IS MISSING FIRST SET IT UP" }
            return TranscriptionResult.Idle
        }

        return when (val result = instance.processSamples(bytes)) {
            is ProcessingState.Buffering -> {
                Logger.d(tag = TAG) { "BUFFERING" }
                TranscriptionResult.Preparing
            }

            is ProcessingState.Error -> {
                Logger.d(tag = TAG) { "FAILED TO PROCESS THE SAMPLES ERROR CODE:${result.errorCode}" }
                TranscriptionResult.Failed(result.errorCode?.toDomainError() ?: TranscriptionError.TranscriptionFailed)
            }

            is ProcessingState.Success -> {
                val state = instance.readState()
                    ?: return TranscriptionResult.Failed(TranscriptionError.TranscriptionFailed)
                Logger.d(tag = TAG) { "GOT SOME SAMPLE RESULT :$state" }
                TranscriptionResult.Success(
                    segment = TranscriptionSegmentModel(segmentId = 0L, state.text),
                )
            }
        }
    }

    actual override fun reset() {

    }

    actual override fun cleanUp() {
        if (_isSetupDone.compareAndSet(expectedValue = true, newValue = false)) {
            Logger.d(tag = TAG) { "AUDIO TRANSCRIPTION SETUP CLOSED" }
            instance.close()
        }
    }

    private fun WhisperErrorCode.toDomainError(): TranscriptionError = when (this.code) {
        101 -> TranscriptionError.UnsupportedAudioFormat
        102 -> TranscriptionError.TranscriptionFailed
        else -> TranscriptionError.TranscriptionFailed
    }

    companion object {
        private const val TAG = "ANDROID_TRANSCRIPTION_ENGINE"
    }
}
