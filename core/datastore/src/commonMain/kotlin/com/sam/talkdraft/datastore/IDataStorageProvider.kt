package com.sam.talkdraft.datastore

import androidx.datastore.core.Storage
import androidx.datastore.preferences.core.Preferences
import okio.Path

internal interface IDataStorageProvider {

    fun provideAppStorage(produceFile: () -> Path): Storage<Preferences>
}
