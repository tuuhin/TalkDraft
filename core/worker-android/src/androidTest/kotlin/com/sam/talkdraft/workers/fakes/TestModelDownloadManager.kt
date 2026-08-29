package com.sam.talkdraft.workers.fakes

import com.sam.talkdraft.model_downloader.domain.IModelDownloadManager
import com.sam.talkdraft.model_downloader.domain.models.ModelDownloadStatus
import kotlin.uuid.Uuid

internal class TestModelDownloadManager : IModelDownloadManager {

    override suspend fun downloadAndSaveModel(
        modelId: Uuid,
        onDownloadState: suspend (ModelDownloadStatus) -> Unit,
    ): Result<Boolean> {
        onDownloadState(ModelDownloadStatus.DownloadInitiated)
        repeat(10) {
            onDownloadState(ModelDownloadStatus.Downloading(it.toFloat()))
        }
        onDownloadState(ModelDownloadStatus.Verifying)
        onDownloadState(ModelDownloadStatus.Success)
        return Result.success(true)
    }
}
