package com.sam.talkdraft.model_downloader.domain.models

sealed class ModelDownloadStatus {
    data object DownloadInitiated : ModelDownloadStatus()
    data class Downloading(val percentage: Int) : ModelDownloadStatus()
    data object Verifying : ModelDownloadStatus()
    data object Success : ModelDownloadStatus()
    data object Failed : ModelDownloadStatus()
}
