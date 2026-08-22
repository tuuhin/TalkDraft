package com.sam.talkdraft.platform_capability.models

data class PlatformCapabilities(
    val memory: PlatformMemoryModel,
    val storage: PlatformStorageModel,
    val processor: PlatformCpuModel,
    val operatingSystem: PlatformOS,
)
