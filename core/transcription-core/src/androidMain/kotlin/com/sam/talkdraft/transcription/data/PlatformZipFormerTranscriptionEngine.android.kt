@file:OptIn(ExperimentalAtomicApi::class)

package com.sam.talkdraft.transcription.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.transcription.data.models.ZipFormerModelPath
import com.sam.talkdraft.transcription.domain.ITranscriptionEngine
import com.sam.talkdraft.transcription.domain.model.TranscriberConfig
import com.sam.talkdraft.transcription.domain.model.TranscriptionState
import com.sam.talkdraft.transcription_android.NativeZipFormer
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

@Factory(binds = [ITranscriptionEngine::class])
@Named(value = "zip_former_engine")
internal actual class PlatformZipFormerTranscriptionEngine(
    private val dispatcher: IPlatformCoroutineDispatchers,
) : ITranscriptionEngine {

    private val instance by lazy { NativeZipFormer() }
    private val _isSetupDone = AtomicBoolean(false)
    private val _lock = Mutex()

    private val fs = FileSystem.SYSTEM

    actual override suspend fun warmUp(request: TranscriberConfig) {
        _lock.withLock {
            if (_isSetupDone.load()) {
                Logger.w(tag = TAG) { "WARMUP IS ALREADY COMPLETED" }
                return
            }
            val success = withContext(dispatcher.default) {
                val modelPath = request.modelPath.toPath()
                // model file/folder itself is missing
                val fileMap = checkAndFetchFileMap(modelPath)
                instance.initialize(
                    encoderPath = fileMap.encoderPath,
                    decoderPath = fileMap.decoderPath,
                    joinerPath = fileMap.joinerPath,
                    tokensPath = fileMap.tokenPath,
                )
            }
            _isSetupDone.compareAndSet(expectedValue = false, success)
            Logger.d(tag = TAG) { "AUDIO TRANSCRIPTION SETUP COMPLETED :$success" }
        }
    }

    actual override fun process(bytes: ShortArray): TranscriptionState {

        if (!_isSetupDone.load()) {
            Logger.w(tag = TAG) { "SETUP IS MISSING FIRST SET IT UP" }
            return TranscriptionState.NotRunning
        }

        val result = instance.processFrame(bytes)
            ?: return TranscriptionState.Success(text = "", segments = emptyList())

        Logger.d(tag = TAG) { "RESULT $result" }

        return TranscriptionState.Success(text = result, emptyList())

    }

    actual override fun cleanUp() {
        if (_isSetupDone.compareAndSet(expectedValue = true, newValue = false)) {
            Logger.d(tag = TAG) { "AUDIO TRANSCRIPTION SETUP CLOSED" }
            instance.close()
        }
    }

    private fun checkAndFetchFileMap(path: Path): ZipFormerModelPath {
        val targetNames = setOf("encoder", "decoder", "joiner", "tokens")

        val foundFiles = fs.listRecursively(path)
            .filter { fs.metadata(it).isRegularFile }
            .mapNotNull { path ->
                // matching based on the start name
                val fileName = path.name.lowercase()
                val matchedPrefix = targetNames.firstOrNull { prefix ->
                    fileName.startsWith(prefix)
                }
                matchedPrefix?.let { it to path }
            }
            .toMap()

        val encoderPath = foundFiles["encoder"] ?: throw IllegalArgumentException("Encoder file missing")
        val decoderPath = foundFiles["decoder"] ?: throw IllegalArgumentException("Decoder file missing")
        val joinerPath = foundFiles["joiner"] ?: throw IllegalArgumentException("Joiner filer missing")
        val tokensPath = foundFiles["tokens"] ?: throw IllegalArgumentException("Tokens file missing")

        val missingFiles = targetNames - foundFiles.keys
        if (missingFiles.isNotEmpty())
            throw IllegalStateException("Missing required model files: $missingFiles in $path")

        return ZipFormerModelPath(encoderPath, decoderPath, joinerPath, tokensPath)
    }

    companion object {
        private const val TAG = "ZIP_FORMER_TRANSCRIPTION_ENGINE"
    }
}
