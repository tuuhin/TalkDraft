package com.sam.talkdraft.common.platform

import android.content.Context
import okio.Path
import okio.Path.Companion.toOkioPath
import org.koin.core.annotation.Singleton

@Singleton(binds = [IPlatformFilePathProvider::class])
internal actual class PlatformFileProviderImpl(
    private val context: Context
) : IPlatformFilePathProvider {

    actual override fun providesDbPath(fileName: String): Path =
        context.getDatabasePath(fileName).toOkioPath()

    actual override fun providesFileDirPath(): Path = context.filesDir.toOkioPath()

    actual override fun providesCachesDirPath(): Path = context.cacheDir.toOkioPath()
}
