package com.sam.talkdraft.model_downloader.domain.models

sealed class ModelDownloadStatus {
    data object DownloadInitiated : ModelDownloadStatus()
    data class Downloading(val percentage: Float) : ModelDownloadStatus()
    data object Verifying : ModelDownloadStatus()
    data object Success : ModelDownloadStatus()
    data class Failed(val message: String? = null) : ModelDownloadStatus()
}
