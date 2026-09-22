package com.sam.talkdraft.file_archive.domain.models

import okio.Path

internal interface IPlatformZipArchiver {
    suspend fun create(srcDirectory: Path, destPath: Path): Result<Unit>
    suspend fun extract(srcPath: Path, destPath: Path): Result<Unit>
}
