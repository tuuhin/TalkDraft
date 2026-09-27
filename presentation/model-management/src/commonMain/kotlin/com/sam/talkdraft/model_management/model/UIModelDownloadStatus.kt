package com.sam.talkdraft.model_management.model

import androidx.compose.runtime.Stable

@Stable
internal sealed class UIModelDownloadStatus {
    data object Idle : UIModelDownloadStatus()
    data object Starting : UIModelDownloadStatus()
    data class Downloading(val progress: () -> Float) : UIModelDownloadStatus()
    data object Verifying : UIModelDownloadStatus()
    data object Extracting : UIModelDownloadStatus()
    data class Failed(val message: String) : UIModelDownloadStatus()
    data object Success : UIModelDownloadStatus()
}
