package com.sam.talkdraft.model_manager.data.mapper

import com.sam.talkdraft.model_manager.domain.local.LocalTranscriptionModel
import com.sam.talkdraft.model_manager.domain.model.LocalModelStatus
import com.sam.talkdraft.model_manager.domain.remote.RemoteTranscriptionModel
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.sam.talkdraft.database.enums.RemoteModelStatus as DbRemoteModelStatus
import com.sam.talkdraft.model_manager.domain.model.RemoteModelStatus as DomainRemoteModelStatus

internal fun RemoteTranscriptionModel.toLocal(timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalTranscriptionModel {
    val remote = this
    return LocalTranscriptionModel(
        id = remote.id,
        modelPath = null,
        status = LocalModelStatus.NOT_INSTALLED,
        downloadedAt = null,
        metadata = LocalTranscriptionModel.Metadata(
            modelFamily = remote.modelFamily,
            variant = remote.variant,
            version = remote.version,
            displayName = remote.displayName,
            source = remote.artifact.source,
            repository = remote.artifact.repository,
            revision = remote.artifact.commitHash,
            artifactPath = remote.artifact.artifactPath,
            languages = remote.artifact.supportedLanguages,
            sizeInBytes = remote.artifact.sizeBytes,
            checksum = remote.artifact.artifactHash,
            remoteStatus = when (remote.remoteStatus) {
                DomainRemoteModelStatus.ACTIVE -> DbRemoteModelStatus.ACTIVE
                DomainRemoteModelStatus.DEPRECATED -> DbRemoteModelStatus.DEPRECATED
                DomainRemoteModelStatus.DISABLED -> DbRemoteModelStatus.INACTIVE
            },
            cachedAt = Clock.System.now().toLocalDateTime(timeZone),
        ),
    )
}
