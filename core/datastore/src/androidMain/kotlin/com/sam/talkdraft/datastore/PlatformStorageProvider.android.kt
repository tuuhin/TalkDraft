package com.sam.talkdraft.datastore

import androidx.datastore.core.FileStorage
import androidx.datastore.core.Storage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesFileSerializer
import okio.Path
import org.koin.core.annotation.Factory

@Factory(binds = [IDataStorageProvider::class])
internal actual class PlatformStorageProvider : IDataStorageProvider {

    actual override fun provideAppStorage(produceFile: () -> Path): Storage<Preferences> {
        val store = FileStorage(serializer = PreferencesFileSerializer, produceFile = { produceFile().toFile() })
        return store
    }
}
