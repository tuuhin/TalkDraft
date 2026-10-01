package com.sam.talkdraft.model_manager.data.mapper

import com.sam.talkdraft.database.entities.TranscriptionModelEntity
import com.sam.talkdraft.database.enums.DBModelArtifactType
import com.sam.talkdraft.database.enums.DBModelDownloadStatus
import com.sam.talkdraft.database.enums.DBModelFamilyOption
import com.sam.talkdraft.database.enums.DBModelTranscriptionType
import com.sam.talkdraft.database.enums.DBRemoteModelStatus
import com.sam.talkdraft.database.relations.LocalTranscriptionModelWithDownloadInfo
import com.sam.talkdraft.model_manager.domain.local.LocalTranscriptionModel
import com.sam.talkdraft.model_manager.domain.model.ModelArtifactType
import com.sam.talkdraft.model_manager.domain.model.ModelInstallStatus
import com.sam.talkdraft.model_manager.domain.model.RemoteModelStatus
import com.sam.talkdraft.model_manager.domain.model.TranscriberFamily
import com.sam.talkdraft.model_manager.domain.model.TranscriptionType
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

internal fun LocalTranscriptionModelWithDownloadInfo.toDomain(timeZone: TimeZone): LocalTranscriptionModel {
    val (metadata, downloadState) = this

    return LocalTranscriptionModel(
        id = metadata.id,
        modelPath = downloadState?.modelPath,
        status = when (downloadState?.modelStatus) {
            DBModelDownloadStatus.UNKNOWN -> ModelInstallStatus.NOT_INSTALLED
            DBModelDownloadStatus.DOWNLOADING -> ModelInstallStatus.DOWNLOADING
            DBModelDownloadStatus.DOWNLOADED -> ModelInstallStatus.INSTALLED
            else -> ModelInstallStatus.NOT_INSTALLED
        },
        downloadedAt = downloadState?.downloadedAt?.toLocalDateTime(timeZone),
        metadata = LocalTranscriptionModel.Metadata(
            modelFamily = when (metadata.modelFamily) {
                DBModelFamilyOption.WHISPER -> TranscriberFamily.WHISPER
                DBModelFamilyOption.ZIP_FORMER -> TranscriberFamily.ZIP_FORMER
                DBModelFamilyOption.UNKNOWN -> TranscriberFamily.UNKNOWN
            },
            variant = metadata.variant,
            version = metadata.version,
            displayName = metadata.displayName,
            description = metadata.description,
            transcriptionType = when (metadata.transcriptionType) {
                DBModelTranscriptionType.STREAMING -> TranscriptionType.STREAMING
                DBModelTranscriptionType.BATCHED -> TranscriptionType.BATCHED
            },
            isDefault = metadata.isDefault,
            remoteStorageType = when (metadata.artifactType) {
                DBModelArtifactType.BINARY -> ModelArtifactType.BINARY
                DBModelArtifactType.PACKAGED -> ModelArtifactType.PACKAGED
                DBModelArtifactType.UNKNOWN -> null
            },
            source = metadata.source,
            repository = metadata.repository,
            revision = metadata.revision,
            artifactPath = metadata.artifactPath,
            languages = metadata.languages,
            sizeInBytes = metadata.sizeInBytes,
            checksum = metadata.checksum,
            remoteStatus = when (metadata.status) {
                DBRemoteModelStatus.ACTIVE -> RemoteModelStatus.ACTIVE
                DBRemoteModelStatus.DEPRECATED -> RemoteModelStatus.DEPRECATED
                DBRemoteModelStatus.INACTIVE -> RemoteModelStatus.DISABLED
            },
            cachedAt = metadata.cachedAt.toLocalDateTime(timeZone),
            lastSync = metadata.lastSync.toLocalDateTime(timeZone),
        ),
    )
}

internal fun LocalTranscriptionModel.toEntity(timeZone: TimeZone = TimeZone.currentSystemDefault()): TranscriptionModelEntity {
    return TranscriptionModelEntity(
        id = id,
        modelFamily = when (metadata.modelFamily) {
            TranscriberFamily.WHISPER -> DBModelFamilyOption.WHISPER
            TranscriberFamily.ZIP_FORMER -> DBModelFamilyOption.ZIP_FORMER
            TranscriberFamily.UNKNOWN -> DBModelFamilyOption.UNKNOWN
        },
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
        status = when (metadata.remoteStatus) {
            RemoteModelStatus.ACTIVE -> DBRemoteModelStatus.ACTIVE
            RemoteModelStatus.DEPRECATED -> DBRemoteModelStatus.DEPRECATED
            RemoteModelStatus.DISABLED -> DBRemoteModelStatus.INACTIVE
        },
        cachedAt = metadata.cachedAt.toInstant(timeZone),
        lastSync = metadata.lastSync.toInstant(timeZone),
        transcriptionType = when (metadata.transcriptionType) {
            TranscriptionType.BATCHED -> DBModelTranscriptionType.BATCHED
            TranscriptionType.STREAMING -> DBModelTranscriptionType.STREAMING
        },
        isDefault = metadata.isDefault,
        artifactType = when (metadata.remoteStorageType) {
            ModelArtifactType.BINARY -> DBModelArtifactType.BINARY
            ModelArtifactType.PACKAGED -> DBModelArtifactType.PACKAGED
            null -> DBModelArtifactType.UNKNOWN
        },
        description = metadata.description,
    )
}
