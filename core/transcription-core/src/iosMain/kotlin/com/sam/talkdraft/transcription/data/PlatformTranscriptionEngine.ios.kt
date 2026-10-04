@file:OptIn(ExperimentalAtomicApi::class)

package com.sam.talkdraft.transcription.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.transcription.domain.ITranscriptionEngine
import com.sam.talkdraft.transcription.domain.model.TranscriberConfig
import com.sam.talkdraft.transcription.domain.model.TranscriptionEngineOutput
import com.sam.talkdraft.transcription.ios.IosNativeWhisper
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

    private val instance by lazy { IosNativeWhisper() }
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
                instance.init(request.modelPath, language)
            }
            _isSetupDone.compareAndSet(expectedValue = false, success)
            Logger.d(tag = TAG) { "AUDIO TRANSCRIPTION SETUP COMPLETED :$success" }
        }
    }

    actual override fun processSegment(bytes: ShortArray): TranscriptionEngineOutput {
        return TranscriptionEngineOutput.Buffering
    }

    actual override fun reset() {
    }

    actual override fun cleanUp() {
        instance.close()
    }

    companion object {
        private const val TAG = "IOS_WHISPER_ENGINE"
    }

}
