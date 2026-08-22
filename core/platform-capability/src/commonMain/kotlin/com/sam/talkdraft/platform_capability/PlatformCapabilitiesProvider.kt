package com.sam.talkdraft.platform_capability

import com.sam.talkdraft.platform_capability.models.PlatformCapabilities
import org.koin.core.annotation.Factory

@Factory(binds = [IPlatformCapabilitiesProvider::class])
internal expect class PlatformCapabilitiesProvider : IPlatformCapabilitiesProvider {
    override suspend fun getCapabilities(): Result<PlatformCapabilities>
}
