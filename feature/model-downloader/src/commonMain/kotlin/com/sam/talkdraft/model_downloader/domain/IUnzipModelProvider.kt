package com.sam.talkdraft.model_downloader.domain

import okio.Path

internal fun interface IUnzipModelProvider {
    suspend fun unzipFilePath(zipFilePath: Path)
}
