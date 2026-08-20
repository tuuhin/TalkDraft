package com.sam.talkdraft.database

import androidx.room3.ColumnTypeConverters
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.sam.talkdraft.database.converters.DurationToLongConvertor
import com.sam.talkdraft.database.converters.InstantToLongConvertor
import com.sam.talkdraft.database.converters.UuidToStringConvertor
import com.sam.talkdraft.database.dao.GeneratedNotesDao
import com.sam.talkdraft.database.dao.LocalTranscriptModelsDao
import com.sam.talkdraft.database.dao.ProcessingJobDao
import com.sam.talkdraft.database.dao.RecordingDao
import com.sam.talkdraft.database.dao.RecordingsDetailsDao
import com.sam.talkdraft.database.dao.TranscriptSegmentsDao
import com.sam.talkdraft.database.dao.TranscriptsDao
import com.sam.talkdraft.database.entities.GeneratedNotesEntity
import com.sam.talkdraft.database.entities.JobProcessingEntity
import com.sam.talkdraft.database.entities.LocalTranscriptModelEntity
import com.sam.talkdraft.database.entities.RecordingEntity
import com.sam.talkdraft.database.entities.TranScriptEntity
import com.sam.talkdraft.database.entities.TranScriptSegmentsEntity
import com.sam.talkdraft.database.utils.TalkDraftDbConstructor
import kotlinx.atomicfu.atomic

@Database(
    entities = [
        GeneratedNotesEntity::class,
        JobProcessingEntity::class,
        RecordingEntity::class,
        TranScriptEntity::class,
        LocalTranscriptModelEntity::class,
        TranScriptSegmentsEntity::class,
    ],
    version = 1,
    autoMigrations = [],
)
@ColumnTypeConverters(
    value = [
        DurationToLongConvertor::class,
        InstantToLongConvertor::class,
        UuidToStringConvertor::class,
    ],
)
@ConstructedBy(TalkDraftDbConstructor::class)
internal abstract class TalkDraftDB : RoomDatabase() {

    abstract fun generatedNotesDao(): GeneratedNotesDao
    abstract fun transcriptsDao(): TranscriptsDao
    abstract fun recordingsDao(): RecordingDao
    abstract fun recordingsDetailsDao(): RecordingsDetailsDao
    abstract fun transcriptsModelDao(): LocalTranscriptModelsDao
    abstract fun transcriptsSegmentDao(): TranscriptSegmentsDao
    abstract fun processingDao(): ProcessingJobDao

    companion object {

        const val APP_DB_NAME = "talk_draft_database.db"

        private val databaseRef = atomic<TalkDraftDB?>(null)

        fun prepareRoomDb(builder: Builder<TalkDraftDB>): TalkDraftDB {
            val dbValue = databaseRef.value
            if (dbValue != null) return dbValue

            val instance = builder
                .build()

            // if db is not set the db
            val setDb = databaseRef.compareAndSet(null, instance)
            if (!setDb) return databaseRef.value!!
            return instance
        }
    }
}
