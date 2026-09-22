package com.sam.talkdraft.file_archive.data

import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.file_archive.domain.IPlatformCompressor
import com.sam.talkdraft.file_archive.domain.models.CompressionAlgo
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import okio.buffer
import okio.gzip
import okio.use
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

@Factory(binds = [IPlatformCompressor::class])
@Named(value = "gzip_compressor", type = CompressionAlgo.GZip::class)
internal class GZipCompressor(private val dispatchers: IPlatformCoroutineDispatchers) : IPlatformCompressor {

    val fs = FileSystem.SYSTEM

    override suspend fun compress(srcPath: Path, destPath: Path) {
        withContext(dispatchers.io) {
            destPath.parent?.let { parent -> fs.createDirectories(parent) }
            fs.source(srcPath).buffer().use { input ->
                // destination is gzipped
                fs.sink(destPath).gzip().buffer().use { gzipOutput ->
                    gzipOutput.writeAll(input)
                }
            }
        }
    }

    override suspend fun decompress(srcPath: Path, destPath: Path) {
        withContext(dispatchers.io) {
            destPath.parent?.let { parent -> fs.createDirectories(parent) }

            // src is gzipped
            fs.source(srcPath).gzip().buffer().use { gzipInput ->
                fs.sink(destPath).buffer().use { output ->
                    output.writeAll(gzipInput)
                }
            }
        }
    }
}
