package com.sam.talkdraft.model_downloader.domain

import io.ktor.utils.io.ByteReadChannel
import okio.Path

internal interface IDownloadTempFileManager {

    /**
     * Streams incoming network data into a temporary cache location.
     */
    suspend fun saveToCache(channel: ByteReadChannel, fileName: String? = null): Result<Path>

    /**
     * Cleans up temporary or corrupted download files.
     */
    suspend fun clearCache(path: Path): Result<Boolean>
}
