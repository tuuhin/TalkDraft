package com.sam.talkdraft.database.di

import com.sam.talkdraft.database.TalkDraftDB
import com.sam.talkdraft.database.utils.AppDBBuilder
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module(includes = [DaoModule::class])
@ComponentScan("com.sam.talkdraft.database")
class DBModule {

    @Singleton
    internal fun providesDb(builder: AppDBBuilder): TalkDraftDB =
        TalkDraftDB.prepareRoomDb(builder.getDbBuilder())
}
