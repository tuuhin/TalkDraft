package com.sam.talkdraft.file_archive.domain

import okio.Path

internal interface IPlatformCompressor {

    suspend fun compress(srcPath: Path, destPath: Path)

    suspend fun decompress(srcPath: Path, destPath: Path)
}
