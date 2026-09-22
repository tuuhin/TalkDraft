package com.sam.talkdraft.file_archive.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.file_archive.domain.models.IPlatformZipArchiver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import no.synth.kmpzip.okio.unzipFrom
import no.synth.kmpzip.okio.zipTo
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import org.koin.core.annotation.Factory

@Factory(binds = [IPlatformZipArchiver::class])
internal class PlatformZipArchiverImpl(
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IPlatformZipArchiver {

    private val fs = FileSystem.SYSTEM

    override suspend fun extract(srcPath: Path, destPath: Path): Result<Unit> =
        withContext(dispatchers.io) {
            var isExtractionSuccessful = false
            try {
                fs.unzipFrom(archive = srcPath, target = destPath)
                isExtractionSuccessful = true
                Result.success(Unit)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.w(tag = TAG, throwable = e) { "FAILED TO UNZIP FILES" }
                Result.failure(e)
            } finally {
                if (!isExtractionSuccessful) {
                    withContext(NonCancellable) {
                        if (fs.exists(destPath)) fs.deleteRecursively(destPath)
                    }
                }
            }
        }

    override suspend fun create(srcDirectory: Path, destPath: Path): Result<Unit> =
        withContext(dispatchers.io) {
            var isArchivingSuccessful = false
            try {
                destPath.parent?.let { fs.createDirectories(it) }
                fs.zipTo(target = destPath, sources = listOf(srcDirectory))
                isArchivingSuccessful = true
                Result.success(Unit)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Logger.w(tag = TAG, throwable = e) { "FAILED TO CREATE ZIP ARCHIVE" }
                Result.failure(e)
            } finally {
                if (!isArchivingSuccessful) {
                    withContext(NonCancellable) {
                        if (fs.exists(destPath)) fs.delete(destPath)
                    }
                }
            }
        }


    companion object {
        private const val TAG = "PlatformZipArchiver"
    }
}
