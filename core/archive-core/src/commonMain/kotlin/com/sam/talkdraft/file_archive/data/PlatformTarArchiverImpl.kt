package com.sam.talkdraft.file_archive.data

import com.sam.talkdraft.file_archive.domain.IPlatformTarArchiver
import okio.Path
import org.koin.core.annotation.Factory

@Factory(binds = [IPlatformTarArchiver::class])
internal expect class PlatformTarArchiverImpl : IPlatformTarArchiver {
    override suspend fun createTar(srcDirectory: Path, destPath: Path): Result<Unit>
    override suspend fun extractTar(srcPath: Path, destPath: Path): Result<Unit>
}
