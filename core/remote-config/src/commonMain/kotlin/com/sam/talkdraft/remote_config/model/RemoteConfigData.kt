package com.sam.talkdraft.remote_config.model

data class RemoteConfigData<T>(
    val key: String = "",
    val value: T? = null,
    val isFlagPresent: Boolean = false,
)
