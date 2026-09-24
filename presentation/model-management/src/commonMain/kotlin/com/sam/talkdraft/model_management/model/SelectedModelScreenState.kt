package com.sam.talkdraft.model_management.model

import com.sam.talkdraft.model_manager.domain.model.ModelInstallStatus
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel

internal data class SelectedModelScreenState(
    val model: TranscriptionModel? = null,
    val downloadStatus: UIModelDownloadStatus = UIModelDownloadStatus.Idle,
    val isModelLoaded: Boolean = false,
) {

    val isModelPresent: Boolean
        get() = model?.status == ModelInstallStatus.INSTALLED && model.modelPath != null

    val isModelSetupRunning: Boolean
        get() = downloadStatus != UIModelDownloadStatus.Idle
}
