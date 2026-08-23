package com.sam.talkdraft.workers

import com.sam.talkdraft.model_downloader.domain.models.ModelDownloadStatus
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

interface IModelDownloadRegistrar {

    fun startModelDownload(model: TranscriptionModel): Uuid

    fun observerDownloadStatus(uuid: Uuid): Flow<ModelDownloadStatus>

    fun cancelDownload(uuid: Uuid)
}
