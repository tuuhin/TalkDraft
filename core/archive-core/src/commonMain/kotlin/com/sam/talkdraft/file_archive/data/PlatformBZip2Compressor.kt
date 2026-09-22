package com.sam.talkdraft.file_archive.data

import com.sam.talkdraft.file_archive.domain.IPlatformCompressor
import com.sam.talkdraft.file_archive.domain.models.CompressionAlgo
import okio.Path
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

@Factory(binds = [IPlatformCompressor::class])
@Named(value = "bzip2_compressor", type = CompressionAlgo.BZip2::class)
internal expect class PlatformBZip2Compressor : IPlatformCompressor {
    override suspend fun compress(srcPath: Path, destPath: Path)
    override suspend fun decompress(srcPath: Path, destPath: Path)
}
