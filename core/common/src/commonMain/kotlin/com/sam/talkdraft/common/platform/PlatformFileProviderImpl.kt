package com.sam.talkdraft.common.platform

import okio.Path
import org.koin.core.annotation.Singleton

@Singleton(binds = [IPlatformFilePathProvider::class])
internal expect class PlatformFileProviderImpl : IPlatformFilePathProvider {
    override fun providesDbPath(fileName: String): Path
    override fun providesFileDirPath(): Path
    override fun providesCachesDirPath(): Path
}
