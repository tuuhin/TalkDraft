package com.sam.talkdraft.model_manager.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class RemoteCatalogVersionDto(
    @SerialName("version") val version: Int,
)
