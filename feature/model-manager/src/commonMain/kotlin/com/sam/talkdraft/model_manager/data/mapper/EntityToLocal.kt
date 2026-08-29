package com.sam.talkdraft.model_manager.data.mapper

import com.sam.talkdraft.database.entities.TranscriptionModelEntity
import com.sam.talkdraft.database.enums.ModelDownloadStatus
import com.sam.talkdraft.database.relations.LocalTranscriptionModelWithDownloadInfo
import com.sam.talkdraft.model_manager.domain.local.LocalTranscriptionModel
import com.sam.talkdraft.model_manager.domain.model.LocalModelStatus
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

internal fun LocalTranscriptionModelWithDownloadInfo.toDomain(timeZone: TimeZone): LocalTranscriptionModel {
    val (metadata, downloadState) = this

    return LocalTranscriptionModel(
        id = metadata.id,
        modelPath = downloadState?.modelPath,
        status = when (downloadState?.modelStatus) {
            ModelDownloadStatus.UNKNOWN -> LocalModelStatus.NOT_INSTALLED
            ModelDownloadStatus.DOWNLOADING -> LocalModelStatus.DOWNLOADING
            ModelDownloadStatus.DOWNLOADED -> LocalModelStatus.INSTALLED
            else -> LocalModelStatus.NOT_INSTALLED
        },
        downloadedAt = downloadState?.downloadedAt?.toLocalDateTime(timeZone),
        metadata = LocalTranscriptionModel.Metadata(
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
            remoteStatus = metadata.status,
            cachedAt = metadata.cachedAt.toLocalDateTime(timeZone),
        ),
    )
}

internal fun LocalTranscriptionModel.toEntity(timeZone: TimeZone): TranscriptionModelEntity {
    return TranscriptionModelEntity(
        id = id,
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
        cachedAt = metadata.cachedAt.toInstant(timeZone),
        lastSync = Clock.System.now(),
    )
}
