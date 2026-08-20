package com.sam.talkdraft.database.utils

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.common.platform.IPlatformFilePathProvider
import com.sam.talkdraft.database.TalkDraftDB
import org.koin.core.annotation.Singleton

@Singleton
internal actual class AppDBBuilder(
    private val context: Context,
    private val platformFs: IPlatformFilePathProvider,
    private val dispatchers: IPlatformCoroutineDispatchers,
) {

    actual fun getDbBuilder(): RoomDatabase.Builder<TalkDraftDB> {

        val appContext = context.applicationContext
        val dbPath = platformFs.providesDbPath(TalkDraftDB.APP_DB_NAME).toFile().absolutePath

        return Room.databaseBuilder<TalkDraftDB>(context = appContext, name = dbPath)
            .setDriver(AndroidSQLiteDriver())
            .setQueryCoroutineContext(dispatchers.io)

    }

    actual fun getMemoryDbBuilder(): RoomDatabase.Builder<TalkDraftDB> {
        return Room.inMemoryDatabaseBuilder<TalkDraftDB>()
            .setDriver(AndroidSQLiteDriver())
            .setQueryCoroutineContext(dispatchers.io)
    }
}
