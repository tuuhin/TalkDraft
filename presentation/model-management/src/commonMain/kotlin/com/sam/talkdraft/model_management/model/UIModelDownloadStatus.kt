package com.sam.talkdraft.model_management.model

internal sealed class UIModelDownloadStatus {
    data object Idle : UIModelDownloadStatus()
    data object Starting : UIModelDownloadStatus()
    data class Downloading(val progress: () -> Float) : UIModelDownloadStatus()
    data object Verifying : UIModelDownloadStatus()
    data class Failed(val message: String) : UIModelDownloadStatus()
    data object Success : UIModelDownloadStatus()
}
