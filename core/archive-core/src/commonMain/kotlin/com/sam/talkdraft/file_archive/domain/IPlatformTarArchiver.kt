package com.sam.talkdraft.file_archive.domain

import okio.Path

internal interface IPlatformTarArchiver {

    suspend fun createTar(srcDirectory: Path, destPath: Path): Result<Unit>
    suspend fun extractTar(srcPath: Path, destPath: Path): Result<Unit>
}
