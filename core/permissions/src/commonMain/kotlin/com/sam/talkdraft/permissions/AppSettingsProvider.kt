package com.sam.talkdraft.permissions

import org.koin.core.annotation.Factory

@Factory(binds = [IAppSettingsProvider::class])
internal expect class AppSettingsProvider : IAppSettingsProvider {
    override suspend fun openSettings()
}
