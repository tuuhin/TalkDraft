package com.sam.talkdraft.model_manager.domain.local

import com.sam.talkdraft.database.enums.RemoteModelStatus
import com.sam.talkdraft.model_manager.domain.model.LocalModelStatus
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDateTime

internal data class LocalTranscriptionModel(
    val id: Uuid,
    val modelPath: String? = null,
    val modelStatus: LocalModelStatus = LocalModelStatus.NOT_INSTALLED,
    val downloadedAt: LocalDateTime? = null,
    val metadata: Metadata,
) {
    data class Metadata(
        val modelFamily: String,
        val variant: String,
        val version: String,
        val displayName: String,
        val source: String,
        val repository: String,
        val revision: String,
        val artifactPath: String,
        val languages: List<String>,
        val sizeInBytes: Long,
        val checksum: String,
        val status: RemoteModelStatus,
        val cachedAt: LocalDateTime,
    )
}
