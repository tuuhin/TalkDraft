package com.sam.talkdraft.model_manager.data.remote.dto

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RemoteModelArtifactDTO(
    @SerialName("id") val id: Uuid,
    @SerialName("catalog_id") val catalogId: Uuid,
    @SerialName("source") val source: RemoteModelProviderDto,
    @SerialName("repository") val repository: String,
    @SerialName("revision") val revision: String,
    @SerialName("artifact_path") val artifactPath: String,
    @SerialName("format") val format: RemoteModelFormatDto,
    @SerialName("languages") val languages: List<String> = emptyList(),
    @SerialName("size_bytes") val sizeBytes: Long,
    @SerialName("sha256") val sha256: String,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") val updatedAt: Instant,
)
