package com.sam.talkdraft.common.platform

import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path
import okio.Path.Companion.toPath
import org.koin.core.annotation.Singleton
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
@Singleton(binds = [IPlatformFilePathProvider::class])
internal actual class PlatformFileProviderImpl : IPlatformFilePathProvider {

    private val fs = NSFileManager.defaultManager

    actual override fun providesDbPath(fileName: String): Path {
        val appSupportURL = fs.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
        val basePath = requireNotNull(appSupportURL?.path).toPath()
        return basePath.resolve(fileName)
    }

    actual override fun providesFileDirPath(): Path {
        val filesURL = fs.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            error = null,
            create = true,
        )
        return requireNotNull(filesURL?.path?.toPath())
    }

    actual override fun providesCachesDirPath(): Path {
        val cachesURL = fs.URLForDirectory(
            directory = NSCachesDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            error = null,
            create = true,
        )

        return requireNotNull(cachesURL?.path?.toPath())
    }
}
