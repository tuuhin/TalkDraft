package com.sam.talkdraft.model_manager.domain.model

import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDateTime

data class TranscriptionModel(
    val id: Uuid,
    val version: String,
    val displayName: String,
    val modelFamily: TranscriberFamily = TranscriberFamily.UNKNOWN,
    val description: String? = null,
    val sizeInBytes: Long = 0L,
    val checksum: String? = null,
    val modelPath: String? = null,
    val supportedLanguages: List<String> = emptyList(),
    val downloadedAt: LocalDateTime? = null,
    val remoteStatus: RemoteModelStatus = RemoteModelStatus.ACTIVE,
    val status: ModelInstallStatus = ModelInstallStatus.NOT_INSTALLED,
    val cachedAt: LocalDateTime,
) {

    internal var artifactPath: String = ""
    internal var variant: String = ""
    internal var source: String = ""
    internal var revision: String = ""
    internal var repository: String = ""
    internal var artifactType: ModelArtifactType = ModelArtifactType.PACKAGED

    val downloadURL: String?
        get() = when (modelFamily) {
            // provided via hugging face
            TranscriberFamily.WHISPER -> "${source}/${repository}/resolve/${revision}/${artifactPath}"
            // provided via GitHub
            TranscriberFamily.ZIP_FORMER -> "${source}/$repository/$revision/$artifactPath"
            else -> null
        }

    val isUnzipRequired: Boolean
        get() = artifactType == ModelArtifactType.PACKAGED

    val isAllLanguageSupported: Boolean
        get() = supportedLanguages.size == 1 && supportedLanguages.contains("*")
}
