package com.sam.talkdraft.model_manager.domain.model

import com.sam.talkdraft.database.enums.RemoteModelStatus
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDateTime

data class TranscriptionModel(
    val id: Uuid,
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
    val modelPath: String? = null,
    val status: RemoteModelStatus,
    val modelStatus: LocalModelStatus,
    val downloadedAt: LocalDateTime? = null,
)
