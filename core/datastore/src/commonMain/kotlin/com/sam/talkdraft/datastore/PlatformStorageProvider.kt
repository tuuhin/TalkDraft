package com.sam.talkdraft.datastore

import androidx.datastore.core.Storage
import androidx.datastore.preferences.core.Preferences
import okio.Path
import org.koin.core.annotation.Factory

@Factory(binds = [IDataStorageProvider::class])
internal expect class PlatformStorageProvider : IDataStorageProvider {
    override fun provideAppStorage(produceFile: () -> Path): Storage<Preferences>
}
