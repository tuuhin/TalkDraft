package com.sam.talkdraft.database.utils

import androidx.room3.RoomDatabase
import com.sam.talkdraft.database.TalkDraftDB
import org.koin.core.annotation.Singleton

@Singleton
internal expect class AppDBBuilder {

    fun getDbBuilder(): RoomDatabase.Builder<TalkDraftDB>

    fun getMemoryDbBuilder(): RoomDatabase.Builder<TalkDraftDB>
}
