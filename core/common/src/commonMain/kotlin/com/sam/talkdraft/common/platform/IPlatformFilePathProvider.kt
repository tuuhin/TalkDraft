package com.sam.talkdraft.common.platform

import okio.Path

interface IPlatformFilePathProvider {
	fun providesFileDirPath(): Path
	fun providesDbPath(fileName: String): Path
	fun providesCachesDirPath(): Path
}
