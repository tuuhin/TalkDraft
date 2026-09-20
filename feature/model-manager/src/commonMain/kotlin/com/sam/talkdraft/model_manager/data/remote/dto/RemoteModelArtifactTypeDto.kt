package com.sam.talkdraft.model_manager.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class RemoteModelArtifactTypeDto {

    @SerialName("BINARY")
    BINARY,

    @SerialName("ZIP")
    ZIP,

    @SerialName("TAR_BZ2")
    TAR_BZ2,
}
