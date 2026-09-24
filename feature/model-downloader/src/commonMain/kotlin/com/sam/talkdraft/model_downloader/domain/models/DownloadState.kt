package com.sam.talkdraft.model_downloader.domain.models

sealed interface DownloadState {
    data object Initiated : DownloadState
    data class Downloading(val progress: Float) : DownloadState
    data object Verifying : DownloadState
    data object Extracting : DownloadState
    data object Success : DownloadState
    data class Failed(val message: String? = null) : DownloadState
}
