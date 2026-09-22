package com.sam.talkdraft.file_archive.data

import com.sam.talkdraft.archive_android.NativeTarArchiver
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.file_archive.domain.IPlatformTarArchiver
import kotlinx.coroutines.withContext
import okio.Path
import org.koin.core.annotation.Factory

@Factory(binds = [IPlatformTarArchiver::class])
internal actual class PlatformTarArchiverImpl(
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IPlatformTarArchiver {

    private val archiver by lazy { NativeTarArchiver() }

    actual override suspend fun createTar(srcDirectory: Path, destPath: Path): Result<Unit> {
        return withContext(dispatchers.io) {
            archiver.createTar(srcDirectory, destPath)
        }
    }

    actual override suspend fun extractTar(srcPath: Path, destPath: Path): Result<Unit> {
        return withContext(dispatchers.io) {
            archiver.extractTar(srcPath, destPath)
        }
    }
}
