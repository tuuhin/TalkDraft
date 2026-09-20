package com.sam.talkdraft.model_manager.data.mapper

import com.sam.talkdraft.model_manager.domain.local.LocalTranscriptionModel
import com.sam.talkdraft.model_manager.domain.model.ModelArtifactType
import com.sam.talkdraft.model_manager.domain.model.ModelInstallStatus
import com.sam.talkdraft.model_manager.domain.remote.RemoteTranscriptionModel
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal fun RemoteTranscriptionModel.toLocal(
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): LocalTranscriptionModel {
    val remote = this
    val now = Clock.System.now()

    return LocalTranscriptionModel(
        id = remote.id,
        modelPath = null,
        status = ModelInstallStatus.NOT_INSTALLED,
        downloadedAt = null,
        metadata = LocalTranscriptionModel.Metadata(
            modelFamily = remote.modelFamily,
            variant = remote.variant,
            version = remote.version,
            displayName = remote.displayName,
            description = remote.description,
            transcriptionType = remote.transcriptionType,
            isDefault = remote.isDefault,
            remoteStorageType = if (remote.artifact.isPackaged) ModelArtifactType.PACKAGED
            else ModelArtifactType.BINARY,
            source = remote.artifact.source,
            repository = remote.artifact.repository,
            revision = remote.artifact.commitHash,
            artifactPath = remote.artifact.artifactPath,
            languages = remote.artifact.supportedLanguages,
            sizeInBytes = remote.artifact.sizeBytes,
            checksum = remote.artifact.artifactHash,
            remoteStatus = remote.remoteStatus,
            cachedAt = now.toLocalDateTime(timeZone),
            lastSync = now.toLocalDateTime(timeZone),
        ),
    )
}
