package com.sam.talkdraft.platform_capability

import com.sam.talkdraft.platform_capability.models.PlatformCapabilities

interface IPlatformCapabilitiesProvider {
    suspend fun getCapabilities(): Result<PlatformCapabilities>
}
