package com.sam.talkdraft.model_manager.data.mapper

import com.sam.talkdraft.model_manager.data.remote.dto.RemoteASRTypeDTO
import com.sam.talkdraft.model_manager.data.remote.dto.RemoteModelArtifactDTO
import com.sam.talkdraft.model_manager.data.remote.dto.RemoteModelArtifactTypeDto
import com.sam.talkdraft.model_manager.data.remote.dto.RemoteModelFamilyDto
import com.sam.talkdraft.model_manager.data.remote.dto.RemoteModelMetadataDTO
import com.sam.talkdraft.model_manager.data.remote.dto.RemoteModelStatusDto
import com.sam.talkdraft.model_manager.domain.model.RemoteModelStatus
import com.sam.talkdraft.model_manager.domain.model.TranscriberFamily
import com.sam.talkdraft.model_manager.domain.model.TranscriptionType
import com.sam.talkdraft.model_manager.domain.remote.RemoteTranscriptionModel

internal fun List<RemoteModelMetadataDTO>.toDomainModels(
    artifacts: List<RemoteModelArtifactDTO>,
): List<RemoteTranscriptionModel> {
    val artifactMap = artifacts.associateBy { it.catalogId }
    return mapNotNull { metadata ->
        val artifact = artifactMap[metadata.id] ?: return@mapNotNull null

        RemoteTranscriptionModel(
            id = metadata.id,
            modelFamily = when (metadata.modelFamily) {
                RemoteModelFamilyDto.WHISPER -> TranscriberFamily.WHISPER
                RemoteModelFamilyDto.ZIP_FORMER -> TranscriberFamily.ZIP_FORMER
            },
            variant = metadata.variant,
            version = metadata.localVersion,
            displayName = metadata.displayName,
            description = metadata.description,
            createdAt = metadata.createdAt,
            updatedAt = metadata.updatedAt,
            isDefault = metadata.isDefault,
            transcriptionType = when (metadata.transcriptionType) {
                RemoteASRTypeDTO.BATCHED -> TranscriptionType.BATCHED
                RemoteASRTypeDTO.STREAMING -> TranscriptionType.STREAMING
            },
            remoteStatus = when (metadata.status) {
                RemoteModelStatusDto.ACTIVE -> RemoteModelStatus.ACTIVE
                RemoteModelStatusDto.DEPRECATED -> RemoteModelStatus.DEPRECATED
                RemoteModelStatusDto.DISABLED -> RemoteModelStatus.DISABLED
            },
            artifact = RemoteTranscriptionModel.Artifact(
                artifactId = artifact.id,
                // mapped to url source
                source = artifact.source.providerURL,
                repository = artifact.repository,
                artifactPath = artifact.artifactPath,
                supportedLanguages = artifact.languages,
                sizeBytes = artifact.sizeBytes,
                commitHash = artifact.revision,
                artifactHash = artifact.sha256,
                isPackaged = artifact.artifact != RemoteModelArtifactTypeDto.BINARY,
            ),
        )
    }
}
