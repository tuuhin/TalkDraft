package com.sam.talkdraft.model_manager.data.mapper

import com.sam.talkdraft.model_manager.data.remote.dto.RemoteModelArtifactDTO
import com.sam.talkdraft.model_manager.data.remote.dto.RemoteModelMetadataDTO
import com.sam.talkdraft.model_manager.data.remote.dto.RemoteModelStatusDto
import com.sam.talkdraft.model_manager.domain.model.RemoteModelStatus
import com.sam.talkdraft.model_manager.domain.remote.RemoteTranscriptionModel

internal fun List<RemoteModelMetadataDTO>.toDomainModels(artifacts: List<RemoteModelArtifactDTO>)
    : List<RemoteTranscriptionModel> {
    val artifactMap = artifacts.associateBy { it.catalogId }
    return mapNotNull { metadata ->
        val artifact = artifactMap[metadata.id] ?: return@mapNotNull null

        RemoteTranscriptionModel(
            id = metadata.id,
            modelFamily = metadata.modelFamily,
            variant = metadata.variant,
            version = metadata.version,

            displayName = metadata.displayName,
            remoteStatus = when (metadata.status) {
                RemoteModelStatusDto.ACTIVE -> RemoteModelStatus.ACTIVE
                RemoteModelStatusDto.DEPRECATED -> RemoteModelStatus.DEPRECATED
                RemoteModelStatusDto.DISABLED -> RemoteModelStatus.DISABLED
            },
            artifact = RemoteTranscriptionModel.Artifact(
                artifactId = artifact.id,
                source = artifact.source.providerURL,
                repository = artifact.repository,
                artifactPath = artifact.artifactPath,
                supportedLanguages = artifact.languages,
                sizeBytes = artifact.sizeBytes,
                commitHash = artifact.revision,
                artifactHash = artifact.sha256,
            ),
        )
    }
}
