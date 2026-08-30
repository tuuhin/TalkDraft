package com.sam.talkdraft.model_manager.data.mapper

import com.sam.talkdraft.model_manager.domain.local.LocalTranscriptionModel
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel

internal fun LocalTranscriptionModel.toDomainModel(): TranscriptionModel = TranscriptionModel(
    id = id,
    downloadedAt = downloadedAt,
    modelStatus = status,
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
    status = metadata.remoteStatus,
    cachedAt = metadata.cachedAt,
    modelPath = modelPath,
)

internal fun TranscriptionModel.toLocal() = LocalTranscriptionModel(
    id = id,
    modelPath = modelPath,
    status = modelStatus,
    downloadedAt = downloadedAt,
    metadata = LocalTranscriptionModel.Metadata(
        modelFamily = modelFamily,
        variant = variant,
        version = version,
        displayName = displayName,
        source = source,
        repository = repository,
        revision = revision,
        artifactPath = artifactPath,
        languages = languages,
        sizeInBytes = sizeInBytes,
        checksum = checksum,
        cachedAt = cachedAt,
        remoteStatus = status,
    ),
)
