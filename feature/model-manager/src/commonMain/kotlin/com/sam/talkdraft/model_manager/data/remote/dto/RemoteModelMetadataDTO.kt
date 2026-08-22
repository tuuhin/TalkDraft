package com.sam.talkdraft.model_manager.data.remote.dto

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RemoteModelMetadataDTO(
    @SerialName("id") val id: Uuid,
    @SerialName("model_family") val modelFamily: String,
    @SerialName("variant") val variant: String,
    @SerialName("version") val version: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("status") val status: RemoteModelStatusDto = RemoteModelStatusDto.ACTIVE,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") val updatedAt: Instant,
)

