package com.sam.talkdraft.model_manager.data.remote.dto

import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RemoteModelMetadataDTO(
    @SerialName("id") val id: Uuid,
    @SerialName("model_family") val modelFamily: RemoteModelFamilyDto,
    @SerialName("variant") val variant: String,
    @SerialName("local_version") val localVersion: String = "0.0.0",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("description") val description: String? = null,
    @SerialName("status") val status: RemoteModelStatusDto = RemoteModelStatusDto.ACTIVE,
    @SerialName("transcription_type") val transcriptionType: RemoteASRTypeDTO,
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("updated_at") val updatedAt: Instant,
)

