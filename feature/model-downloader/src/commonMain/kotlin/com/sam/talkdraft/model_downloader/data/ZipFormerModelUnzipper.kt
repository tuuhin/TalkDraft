package com.sam.talkdraft.model_downloader.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.model_downloader.domain.IUnzipModelProvider
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import okio.buffer
import okio.openZip
import okio.use
import org.koin.core.annotation.Factory

private const val TAG = "UnZipModelManager"

@Factory(binds = [IUnzipModelProvider::class])
internal class ZipFormerModelUnzipper(
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IUnzipModelProvider {
    private val fs = FileSystem.SYSTEM

    override suspend fun unzipFilePath(zipFilePath: Path) = withContext(dispatchers.io) {

        val parent = zipFilePath.parent ?: throw IllegalStateException("Parent missing")
        val targetKalidiDir = parent / "kalidi"

        if (!fs.exists(targetKalidiDir)) fs.createDirectories(targetKalidiDir)

        var isExtractionSuccessful = false

        try {
            fs.openZip(zipFilePath).use { zipFileSystem ->
                fs.createDirectories(targetKalidiDir)
                val zipEntries = zipFileSystem.listRecursively("/".toPath())
                for (entry in zipEntries) {
                    val relativePath = entry.relativeTo("/".toPath())
                    val targetPath = targetKalidiDir / relativePath

                    val metadata = zipFileSystem.metadata(entry)
                    if (metadata.isDirectory) fs.createDirectories(targetPath)
                    else
                        targetPath.parent?.let { fs.createDirectories(it) }
                    zipFileSystem.source(entry).buffer().use { source ->
                        fs.sink(targetPath).buffer().use { sink ->
                            sink.writeAll(source)
                        }
                    }
                }
            }
            isExtractionSuccessful = true
            fs.delete(zipFilePath)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Logger.w(tag = TAG, throwable = e) { "FAILED TO UNZIP KALIDI FILES " }
        } finally {
            // in case extraction failed cancel the operation
            if (!isExtractionSuccessful) {
                withContext(NonCancellable) {
                    if (!fs.exists(targetKalidiDir)) return@withContext
                    fs.deleteRecursively(targetKalidiDir)
                }
            }
        }
    }

}
