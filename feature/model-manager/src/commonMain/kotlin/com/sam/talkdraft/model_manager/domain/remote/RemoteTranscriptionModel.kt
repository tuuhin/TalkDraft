package com.sam.talkdraft.model_manager.domain.remote

import com.sam.talkdraft.model_manager.domain.model.RemoteModelStatus
import com.sam.talkdraft.model_manager.domain.model.TranscriberFamily
import com.sam.talkdraft.model_manager.domain.model.TranscriptionType
import kotlin.time.Instant
import kotlin.uuid.Uuid

internal data class RemoteTranscriptionModel(
    val id: Uuid,
    val variant: String,
    val version: String,
    val displayName: String,
    val description: String?,
    val isDefault: Boolean,
    val modelFamily: TranscriberFamily,
    val transcriptionType: TranscriptionType,
    val remoteStatus: RemoteModelStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val artifact: Artifact,
) {
    data class Artifact(
        val artifactId: Uuid,
        val repository: String,
        val commitHash: String,
        val source: String,
        val artifactPath: String,
        val supportedLanguages: List<String>,
        val sizeBytes: Long,
        val isPackaged: Boolean = false,
        val artifactHash: String? = null,
    )
}
