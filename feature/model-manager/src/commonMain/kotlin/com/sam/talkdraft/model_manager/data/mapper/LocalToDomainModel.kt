package com.sam.talkdraft.model_manager.data.mapper

import com.sam.talkdraft.model_manager.domain.local.LocalTranscriptionModel
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel

internal fun LocalTranscriptionModel.toDomainModel(): TranscriptionModel = TranscriptionModel(
    id = id,
    downloadedAt = downloadedAt,
    modelStatus = modelStatus,
    modelFamily = metadata.modelFamily,
    variant = metadata.variant,
    version = metadata.version,
    displayName = metadata.displayName,
    source = metadata.source,
    repository = metadata.repository,
    revision = metadata.revision,
    artifactPath = metadata.artifactPath,
    languages = metadata.languages,
    sizeInBytes = metadata.sizeInBytes,
    checksum = metadata.checksum,
    status = metadata.status,
)
