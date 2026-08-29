package com.sam.talkdraft.model_manager.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class RemoteModelProviderDto {
    @SerialName("HUGGING_FACE")
    HUGGING_FACE;

    val providerURL: String
        get() = when (this) {
            HUGGING_FACE -> "https://huggingface.co"
        }

}
