package com.sam.talkdraft.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

interface IDataStoreProvider {

    fun providesAppDataStore(): DataStore<Preferences>

    fun providesUserDataStore(): DataStore<Preferences>
}
