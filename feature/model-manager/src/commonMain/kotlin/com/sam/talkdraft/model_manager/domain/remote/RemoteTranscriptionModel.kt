package com.sam.talkdraft.model_manager.domain.remote

import com.sam.talkdraft.model_manager.domain.model.RemoteModelStatus
import kotlin.uuid.Uuid

internal data class RemoteTranscriptionModel(
    val id: Uuid,
    val modelFamily: String,
    val variant: String,
    val version: String,
    val displayName: String,
    val remoteStatus: RemoteModelStatus,
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
        val artifactHash: String,
    )
}
