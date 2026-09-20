package com.sam.talkdraft.model_manager.domain.local

import com.sam.talkdraft.model_manager.domain.model.ModelArtifactType
import com.sam.talkdraft.model_manager.domain.model.ModelInstallStatus
import com.sam.talkdraft.model_manager.domain.model.RemoteModelStatus
import com.sam.talkdraft.model_manager.domain.model.TranscriberFamily
import com.sam.talkdraft.model_manager.domain.model.TranscriptionMode
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDateTime

internal data class LocalTranscriptionModel(
    val id: Uuid,
    val modelPath: String? = null,
    val status: ModelInstallStatus = ModelInstallStatus.NOT_INSTALLED,
    val downloadedAt: LocalDateTime? = null,
    val metadata: Metadata,
) {
    data class Metadata(
        val modelFamily: TranscriberFamily = TranscriberFamily.UNKNOWN,
        val variant: String,
        val version: String,
        val displayName: String,
        val description: String?,
        val transcriptionType: TranscriptionMode,
        val isDefault: Boolean,
        val remoteStorageType: ModelArtifactType? = null,
        val source: String,
        val repository: String,
        val revision: String,
        val artifactPath: String,
        val languages: List<String>,
        val sizeInBytes: Long,
        val checksum: String?,
        val remoteStatus: RemoteModelStatus,
        val cachedAt: LocalDateTime,
        val lastSync: LocalDateTime,
    )
}
