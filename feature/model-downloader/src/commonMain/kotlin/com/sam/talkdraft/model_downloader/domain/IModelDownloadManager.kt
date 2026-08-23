package com.sam.talkdraft.model_downloader.domain

import com.sam.talkdraft.model_downloader.domain.models.ModelDownloadStatus
import kotlin.uuid.Uuid

fun interface IModelDownloadManager {

    suspend fun downloadAndSaveModel(modelId: Uuid, onDownloadState: (ModelDownloadStatus) -> Unit): Result<Boolean>
}
