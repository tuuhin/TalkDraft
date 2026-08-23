package com.sam.talkdraft.datastore.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sam.talkdraft.datastore.IDataStoreProvider
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Named
import org.koin.core.annotation.Singleton

@Module
@ComponentScan("com.sam.talkdraft.datastore")
class DataStoreModule {

    @Singleton
    @Named("app_settings")
    fun providesAppSettings(provider: IDataStoreProvider): DataStore<Preferences> = provider.providesAppDataStore()

    @Singleton
    @Named("user_settings")
    fun provideUserSettings(provider: IDataStoreProvider): DataStore<Preferences> = provider.providesUserDataStore()
}
