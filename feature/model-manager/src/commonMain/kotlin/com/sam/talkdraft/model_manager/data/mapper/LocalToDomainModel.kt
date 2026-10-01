package com.sam.talkdraft.model_manager.data.mapper

import com.sam.talkdraft.model_manager.domain.local.LocalTranscriptionModel
import com.sam.talkdraft.model_manager.domain.model.ModelArtifactType
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel

internal fun LocalTranscriptionModel.toDomainModel(): TranscriptionModel {
    val model = TranscriptionModel(
        id = id,
        version = metadata.version,
        displayName = metadata.displayName,
        modelFamily = metadata.modelFamily,
        description = metadata.description,
        sizeInBytes = metadata.sizeInBytes,
        checksum = metadata.checksum,
        modelPath = modelPath,
        supportedLanguages = metadata.languages,
        downloadedAt = downloadedAt,
        remoteStatus = metadata.remoteStatus,
        status = status,
        cachedAt = metadata.cachedAt,
        type = metadata.transcriptionType,
    )

    // Set internal mutable fields
    model.variant = metadata.variant
    model.artifactPath = metadata.artifactPath
    model.source = metadata.source
    model.revision = metadata.revision
    model.repository = metadata.repository
    model.artifactType = metadata.remoteStorageType ?: ModelArtifactType.PACKAGED

    return model
}

internal fun TranscriptionModel.toLocal(): LocalTranscriptionModel = LocalTranscriptionModel(
    id = id,
    modelPath = modelPath,
    status = status,
    downloadedAt = downloadedAt,
    metadata = LocalTranscriptionModel.Metadata(
        modelFamily = modelFamily,
        variant = variant,
        version = version,
        displayName = displayName,
        description = description,
        transcriptionType = type,
        isDefault = false,
        remoteStorageType = artifactType,
        source = source,
        repository = repository,
        revision = revision,
        artifactPath = artifactPath,
        languages = supportedLanguages,
        sizeInBytes = sizeInBytes,
        checksum = checksum,
        cachedAt = cachedAt,
        lastSync = cachedAt,
        remoteStatus = remoteStatus,
    ),
)
