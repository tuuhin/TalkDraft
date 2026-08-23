package com.sam.talkdraft.datastore

import androidx.datastore.core.Storage
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import okio.FileSystem
import okio.Path
import org.koin.core.annotation.Factory

@Factory(binds = [IDataStorageProvider::class])
internal actual class PlatformStorageProvider : IDataStorageProvider {
    actual override fun provideAppStorage(produceFile: () -> Path): Storage<Preferences> {
        return OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = PreferencesSerializer,
            producePath = { produceFile() },
        )
    }
}
