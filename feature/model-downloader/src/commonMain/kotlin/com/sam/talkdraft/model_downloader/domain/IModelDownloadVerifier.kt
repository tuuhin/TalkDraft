package com.sam.talkdraft.model_downloader.domain

import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import okio.Path

internal fun interface IModelDownloadVerifier {

    suspend fun validateModelHash(modelPath: Path, model: TranscriptionModel): Boolean
}
