package com.sam.talkdraft.file_archive.data

import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.file_archive.domain.IPlatformCompressor
import com.sam.talkdraft.file_archive.domain.models.CompressionAlgo
import com.sam.talkdraft.ios_archive.NativeBZip2Compressor
import kotlinx.coroutines.withContext
import okio.Path
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

@Factory(binds = [IPlatformCompressor::class])
@Named(value = "bzip2_compressor", type = CompressionAlgo.BZip2::class)
internal actual class PlatformBZip2Compressor(
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IPlatformCompressor {

    private val compressor by lazy { NativeBZip2Compressor() }

    actual override suspend fun compress(srcPath: Path, destPath: Path) {
        withContext(dispatchers.io) {
            compressor.compress(srcPath, destPath)
        }
    }

    actual override suspend fun decompress(srcPath: Path, destPath: Path) {
        withContext(dispatchers.io) {
            compressor.decompress(srcPath, destPath)
        }
    }
}
