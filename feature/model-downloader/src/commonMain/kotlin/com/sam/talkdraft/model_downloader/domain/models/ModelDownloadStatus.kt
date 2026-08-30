package com.sam.talkdraft.model_downloader.domain.models

import kotlin.uuid.Uuid

data class ModelDownloadStatus(
    val modelId: Uuid,
    val state: DownloadState = DownloadState.Initiated,
)
