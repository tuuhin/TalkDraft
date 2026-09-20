package com.sam.talkdraft.background_jobs.data

import com.sam.talkdraft.background_jobs.IModelDownloadRegistrar
import com.sam.talkdraft.model_downloader.domain.models.ModelDownloadStatus
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

internal expect class ModelDownloadRegistrar : IModelDownloadRegistrar {
    override fun startModelDownload(model: TranscriptionModel): Uuid
    override fun observerDownloadStatus(uuid: Uuid): Flow<ModelDownloadStatus>
    override fun observerDownloadStatus(model: TranscriptionModel): Flow<Pair<Uuid, ModelDownloadStatus>>
    override fun cancelDownload(uuid: Uuid)
}
