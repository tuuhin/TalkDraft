package com.sam.talkdraft.model_manager.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class RemoteModelFamilyDto {

    @SerialName("WHISPER")
    WHISPER,

    @SerialName("ZIPFORMER")
    ZIP_FORMER,
}
