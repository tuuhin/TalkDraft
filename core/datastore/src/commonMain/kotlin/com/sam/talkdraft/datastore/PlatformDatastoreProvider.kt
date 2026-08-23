package com.sam.talkdraft.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.sam.talkdraft.common.platform.IPlatformFilePathProvider
import org.koin.core.annotation.Singleton

@Singleton(binds = [IDataStoreProvider::class])
internal class PlatformDatastoreProvider(
    private val filePathProvider: IPlatformFilePathProvider,
    private val storageProvider: IDataStorageProvider,
) : IDataStoreProvider {

    private val settingsPath by lazy { filePathProvider.providesFileDirPath() / "settings" }


    override fun providesAppDataStore(): DataStore<Preferences> {
        return DataStoreFactory.create(
            storage = storageProvider.provideAppStorage {
                settingsPath / DataStoreConstants.APP_DATASTORE_FILE
            },
        )
    }

    override fun providesUserDataStore(): DataStore<Preferences> {
        return DataStoreFactory.create(
            storage = storageProvider.provideAppStorage {
                settingsPath / DataStoreConstants.USER_DATASTORE_FILE
            },
        )
    }
}
