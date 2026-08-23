package com.sam.talkdraft.model_downloader.domain

import okio.Path
import okio.Source

internal interface IDownloadTempFileManager {

    /**
     * Streams incoming network data into a temporary cache location.
     */
    suspend fun saveToCache(source: Source, fileName: String? = null): Result<Path>

    /**
     * Cleans up temporary or corrupted download files.
     */
    suspend fun clearCache(path: Path): Result<Boolean>
}
