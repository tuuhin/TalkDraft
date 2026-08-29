package com.sam.talkdraft.background_jobs.data

import com.sam.talkdraft.background_jobs.IModelDownloadRegistrar
import com.sam.talkdraft.model_downloader.domain.models.ModelDownloadStatus
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
internal actual class ModelDownloadRegistrar : IModelDownloadRegistrar {
    actual override fun startModelDownload(model: TranscriptionModel): Uuid {
        TODO("Not yet implemented")
    }

    actual override fun observerDownloadStatus(uuid: Uuid): Flow<ModelDownloadStatus> {
        TODO("Not yet implemented")
    }

    actual override fun cancelDownload(uuid: Uuid) {
    }
}
