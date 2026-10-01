package com.sam.talkdraft.model_management.model

import com.sam.talkdraft.connectivity.models.ConnectivityState
import com.sam.talkdraft.model_manager.domain.model.ModelInstallStatus
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel

internal data class SelectedModelScreenState(
    val model: TranscriptionModel? = null,
    val downloadStatus: UIModelDownloadStatus = UIModelDownloadStatus.Idle,
    val isModelLoaded: Boolean = false,
    val networkState: ConnectivityState = ConnectivityState.OFFLINE,
) {

    val isModelPresent: Boolean
        get() = model?.status == ModelInstallStatus.INSTALLED && model.modelPath != null

    val isModelSetupRunning: Boolean
        get() = when (downloadStatus) {
            is UIModelDownloadStatus.Failed, UIModelDownloadStatus.Success, UIModelDownloadStatus.Idle -> false
            else -> true
        }

    val isModelLoadFailed: Boolean
        get() = isModelLoaded && model != null
}
