@file:OptIn(ExperimentalAtomicApi::class)

package com.sam.talkdraft.transcription.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.transcription.domain.ITranscriptionEngine
import com.sam.talkdraft.transcription.domain.model.TranscriberConfig
import com.sam.talkdraft.transcription.domain.model.TranscriptionEngineOutput
import com.sam.talkdraft.transcription.domain.model.TranscriptionError
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
                instance.initialize(request.modelPath, language)
            }
            _isSetupDone.compareAndSet(expectedValue = false, success)
            Logger.d(tag = TAG) { "AUDIO TRANSCRIPTION SETUP COMPLETED :$success" }
        }
    }

    actual override fun processSegment(bytes: ShortArray): TranscriptionEngineOutput {
        if (!_isSetupDone.load()) {
            Logger.w(tag = TAG) { "SETUP IS MISSING FIRST SET IT UP" }
            return TranscriptionEngineOutput.InvalidResult(error = TranscriptionError.ModelSetupAbsent)
        }

        return when (val result = instance.processSamples(bytes)) {
            is ProcessingState.Buffering -> TranscriptionEngineOutput.Buffering

            is ProcessingState.Error -> {
                Logger.d(tag = TAG) { "FAILED TO PROCESS THE SAMPLES ERROR CODE:${result.errorCode}" }
                TranscriptionEngineOutput.InvalidResult(result.errorCode?.toDomainError() ?: TranscriptionError.Unknown)
            }

            is ProcessingState.Success -> {
                val state = instance.readState()
                    ?: return TranscriptionEngineOutput.InvalidResult(TranscriptionError.Unknown)
                Logger.d(tag = TAG) { "GOT SOME SAMPLE RESULT :$state" }
                TranscriptionEngineOutput.Segment(
                    segmentId = 0L,
                    text = state.text,
                    startTime = state.startTime,
                    endTime = state.endTime,
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
