package com.sam.talkdraft.background_jobs.data

import com.sam.talkdraft.background_jobs.IModelDownloadRegistrar
import com.sam.talkdraft.model_downloader.domain.models.DownloadState
import com.sam.talkdraft.model_downloader.domain.models.ModelDownloadStatus
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update
import org.koin.core.annotation.Singleton

@Singleton
internal actual class ModelDownloadRegistrar : IModelDownloadRegistrar {


    private val _activeDownloadId = MutableStateFlow<Uuid?>(null)
    private val _modelId = MutableStateFlow<Uuid?>(null)
    private val _downloadStatus = MutableStateFlow<DownloadState?>(null)

    actual override fun startModelDownload(model: TranscriptionModel): Uuid {
        check(_activeDownloadId.value == null) { "A model download is already in progress" }

        val uuid = Uuid.random()

        _downloadStatus.update { DownloadState.Initiated }

        return Uuid.random()
    }

    actual override fun observerDownloadStatus(uuid: Uuid): Flow<ModelDownloadStatus> {
        return emptyFlow()
    }

    actual override fun observerDownloadStatus(model: TranscriptionModel): Flow<Pair<Uuid, ModelDownloadStatus>> {
        return emptyFlow()
    }

    actual override fun cancelDownload(uuid: Uuid) {

    }
}
