package com.sam.talkdraft.datastore.model

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sam.talkdraft.datastore.IDataStoreProvider
import org.koin.core.annotation.Single

@Single
class AppSettingsDataStore(
    provider: IDataStoreProvider,
) : DataStore<Preferences> by provider.providesAppDataStore()

@Single
class UserSettingsDataStore(
    provider: IDataStoreProvider,
) : DataStore<Preferences> by provider.providesUserDataStore()
