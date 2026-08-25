package com.sam.talkdraft.onboarding.util

import org.koin.core.annotation.Factory

@Factory(binds = [IAppSettingsProvider::class])
internal expect class AppSettingsProvider : IAppSettingsProvider {
    override suspend fun openSettings()
}
