package com.sam.talkdraft.model_downloader.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.common.platform.IPlatformFilePathProvider
import com.sam.talkdraft.common.platform.PlatformRandomNonceGenerator
import com.sam.talkdraft.model_downloader.domain.IDownloadTempFileManager
import io.ktor.utils.io.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import okio.Source
import okio.buffer
import okio.use
import org.koin.core.annotation.Factory

private const val TAG = "TEMP_FILE_MANAGER"

@Factory(binds = [IDownloadTempFileManager::class])
internal class DownloadTempFileManager(
    private val dispatchers: IPlatformCoroutineDispatchers,
    private val fileProvider: IPlatformFilePathProvider,
    private val randomGenerator: PlatformRandomNonceGenerator,
) : IDownloadTempFileManager {

    private val cacheFileProvider by lazy { fileProvider.providesCachesDirPath() / "transcription_models" }
    private val fs = FileSystem.SYSTEM

    override suspend fun saveToCache(source: Source, fileName: String?): Result<Path> {
        return try {
            val path = cacheFileProvider / ((fileName ?: randomGenerator.generateNonce()) + ".tmp")

            withContext(dispatchers.io) {
                if (!fs.exists(path)) {
                    path.parent?.let(fs::createDirectories)
                }
                try {
                    fs.sink(path).buffer().use { sink ->
                        sink.writeAll(source)
                        sink.flush()
                    }
                    Logger.d(tag = TAG) { "FILE SAVED SUCCESSFULLY AT :$path" }
                } catch (e: CancellationException) {
                    Logger.d(tag = TAG) { "OPERATION CANCELLED DELETING THE FILE" }
                    withContext(NonCancellable) {
                        fs.delete(path)
                    }
                    throw e
                }
            }
            Logger.d(tag = TAG) { "FILE HAS BEEN COPIED COPIED SUCCESSFULLY" }
            Result.success(path)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.d(tag = TAG) { "FAILED TO DOWNLOAD AND SAVE FILE" }
            Result.failure(e)
        }
    }

    override suspend fun clearCache(path: Path): Result<Boolean> {
        return withContext(dispatchers.io) {
            if (!fs.exists(path)) return@withContext Result.success(false)
            try {
                fs.delete(path)
                Logger.d(tag = TAG) { "FILE DELETED SUCCESSFULLY :$path" }
                Result.success(true)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.d(tag = TAG, throwable = e) { "FAILED TO DELETE THE FILE" }
                Result.failure(e)
            }
        }
    }
}
