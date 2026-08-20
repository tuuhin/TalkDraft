package com.sam.talkdraft.database.utils

import androidx.room3.RoomDatabaseConstructor
import com.sam.talkdraft.database.TalkDraftDB

@Suppress("KotlinNoActualForExpect")
internal expect object TalkDraftDbConstructor : RoomDatabaseConstructor<TalkDraftDB> {
    override fun initialize(): TalkDraftDB
}
