package com.sam.talkdraft.file_archive.data

import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.common.platform.IPlatformFilePathProvider
import com.sam.talkdraft.file_archive.domain.IArchiveExtractor
import com.sam.talkdraft.file_archive.domain.IPlatformCompressor
import com.sam.talkdraft.file_archive.domain.IPlatformTarArchiver
import com.sam.talkdraft.file_archive.domain.models.ArchiveFormats
import com.sam.talkdraft.file_archive.domain.models.CompressionAlgo
import com.sam.talkdraft.file_archive.domain.models.IPlatformZipArchiver
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

@Factory(binds = [IArchiveExtractor::class])
internal class ArchiveExtractorImpl(
    private val fileProvider: IPlatformFilePathProvider,
    private val dispatchers: IPlatformCoroutineDispatchers,
    private val archiver: IPlatformTarArchiver,
    private val zipArchiver: IPlatformZipArchiver,
    @Named(
        type = CompressionAlgo.GZip::class,
        value = "gzip_compressor",
    )
    private val gzipCompressor: IPlatformCompressor,
    @Named(
        type = CompressionAlgo.BZip2::class,
        value = "bzip2_compressor",
    )
    private val bzip2Compressor: IPlatformCompressor,
) : IArchiveExtractor {

    private val fs = FileSystem.SYSTEM

    override suspend fun extract(src: Path, dest: Path, formats: ArchiveFormats): Result<Unit> {
        return when (formats) {
            ArchiveFormats.ZIP -> zipArchiver.extract(src, dest)
            ArchiveFormats.TAR -> archiver.extractTar(src, dest)
            ArchiveFormats.TAR_BZIP2 -> unCompressViaBzip2AndExtractTar(src, dest)
            ArchiveFormats.TAR_GZIP -> unCompressViaGzip2AndExtractTar(src, dest)
        }
    }

    private suspend fun unCompressViaBzip2AndExtractTar(src: Path, dest: Path): Result<Unit> {
        val destPath = fileProvider.providesCachesDirPath() / "extraction"
        return try {
            bzip2Compressor.decompress(src, destPath)
            archiver.extractTar(destPath, dest)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            withContext(dispatchers.io + NonCancellable) {
                fs.delete(destPath)
            }
        }
    }

    private suspend fun unCompressViaGzip2AndExtractTar(src: Path, dest: Path): Result<Unit> {
        val destPath = fileProvider.providesCachesDirPath() / "extraction"
        return try {
            gzipCompressor.decompress(src, destPath)
            archiver.extractTar(destPath, dest)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            withContext(dispatchers.io + NonCancellable) {
                fs.delete(destPath)
            }
        }
    }

}
