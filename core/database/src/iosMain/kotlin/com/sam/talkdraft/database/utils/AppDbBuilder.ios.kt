package com.sam.talkdraft.database.utils

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.NativeSQLiteDriver
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.common.platform.IPlatformFilePathProvider
import com.sam.talkdraft.database.TalkDraftDB
import org.koin.core.annotation.Singleton

@Singleton
internal actual class AppDBBuilder(
    private val platformFs: IPlatformFilePathProvider,
    private val dispatchers: IPlatformCoroutineDispatchers,
) {

    actual fun getDbBuilder(): RoomDatabase.Builder<TalkDraftDB> {
        val dbPath = platformFs.providesDbPath(TalkDraftDB.APP_DB_NAME)

        return Room.databaseBuilder<TalkDraftDB>(dbPath.toString())
            .setDriver(NativeSQLiteDriver())
            .setQueryCoroutineContext(dispatchers.io)
            .fallbackToDestructiveMigration()
    }

    actual fun getMemoryDbBuilder(): RoomDatabase.Builder<TalkDraftDB> {
        return Room.inMemoryDatabaseBuilder<TalkDraftDB>()
            .setDriver(NativeSQLiteDriver())
            .setQueryCoroutineContext(dispatchers.io)
    }
}
