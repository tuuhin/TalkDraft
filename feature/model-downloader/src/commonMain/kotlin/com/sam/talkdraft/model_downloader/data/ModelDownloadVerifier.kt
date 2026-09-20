package com.sam.talkdraft.model_downloader.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.model_downloader.domain.IModelDownloadVerifier
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.HashingSink
import okio.Path
import okio.SYSTEM
import okio.blackholeSink
import okio.buffer
import okio.use
import org.koin.core.annotation.Factory

private const val TAG = "MODEL_DOWNLOAD_VERIFIER"

@Factory(binds = [IModelDownloadVerifier::class])
internal class ModelDownloadVerifier(
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IModelDownloadVerifier {

    private val fs = FileSystem.SYSTEM

    override suspend fun validateModelHash(modelPath: Path, model: TranscriptionModel): Boolean {
        return withContext(dispatchers.io) {
            // if checksum is not provided then it's a pass
            if (model.checksum == null) return@withContext true
            try {
                val isPresent = fs.exists(modelPath)
                if (!isPresent) return@withContext false

                val sink = HashingSink.sha256(blackholeSink())
                fs.source(modelPath).use { source ->
                    sink.buffer().use { bufferedSink ->
                        bufferedSink.writeAll(source)
                    }
                }
                val readHash = sink.hash.hex()
                Logger.d(tag = TAG) { "DOWNLOADED MODEL HASH :$readHash" }
                return@withContext readHash == model.checksum
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.e(tag = TAG, throwable = e) { "FAILED TO VERIFY HASH" }
                false
            }
        }
    }
}
