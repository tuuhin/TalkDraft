package com.sam.talkdraft.database

import androidx.room3.ColumnTypeConverters
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.sam.talkdraft.database.converters.DurationToLongConvertor
import com.sam.talkdraft.database.converters.InstantToLongConvertor
import com.sam.talkdraft.database.converters.ListToStringConvertor
import com.sam.talkdraft.database.converters.UuidToStringConvertor
import com.sam.talkdraft.database.dao.GeneratedNotesDao
import com.sam.talkdraft.database.dao.LocalTranscriptionEntityDao
import com.sam.talkdraft.database.dao.ProcessingJobDao
import com.sam.talkdraft.database.dao.RecordingDao
import com.sam.talkdraft.database.dao.RecordingsDetailsDao
import com.sam.talkdraft.database.dao.TranscriptModelDownloadEntityDao
import com.sam.talkdraft.database.dao.TranscriptSegmentsDao
import com.sam.talkdraft.database.dao.TranscriptsDao
import com.sam.talkdraft.database.entities.DownloadedTranscriptionModelEntity
import com.sam.talkdraft.database.entities.GeneratedNotesEntity
import com.sam.talkdraft.database.entities.JobProcessingEntity
import com.sam.talkdraft.database.entities.RecordingEntity
import com.sam.talkdraft.database.entities.TranScriptEntity
import com.sam.talkdraft.database.entities.TranScriptSegmentsEntity
import com.sam.talkdraft.database.entities.TranscriptionModelEntity
import com.sam.talkdraft.database.utils.TalkDraftDbConstructor
import kotlinx.atomicfu.atomic

@Database(
    entities = [
        GeneratedNotesEntity::class,
        JobProcessingEntity::class,
        RecordingEntity::class,
        TranScriptEntity::class,
        DownloadedTranscriptionModelEntity::class,
        TranScriptSegmentsEntity::class,
        TranscriptionModelEntity::class,
    ],
    version = 1,
    autoMigrations = [],
)
@ColumnTypeConverters(
    value = [
        DurationToLongConvertor::class,
        InstantToLongConvertor::class,
        UuidToStringConvertor::class,
        ListToStringConvertor::class,
    ],
)
@ConstructedBy(TalkDraftDbConstructor::class)
internal abstract class TalkDraftDB : RoomDatabase() {

    // generated notes
    abstract fun generatedNotesDao(): GeneratedNotesDao

    // recordings and transcripts
    abstract fun transcriptsDao(): TranscriptsDao
    abstract fun recordingsDao(): RecordingDao
    abstract fun recordingsDetailsDao(): RecordingsDetailsDao
    abstract fun transcriptsSegmentDao(): TranscriptSegmentsDao

    // processing audit
    abstract fun processingDao(): ProcessingJobDao

    // local transcriptions model list
    abstract fun transcriptionModelDao(): LocalTranscriptionEntityDao
    abstract fun downloadEntityDao(): TranscriptModelDownloadEntityDao

    companion object {

        const val APP_DB_NAME = "talk_draft_database.db"

        private val databaseRef = atomic<TalkDraftDB?>(null)

        fun prepareRoomDb(builder: Builder<TalkDraftDB>): TalkDraftDB {
            val dbValue = databaseRef.value
            if (dbValue != null) return dbValue

            val instance = builder
                .addColumnTypeConverter(UuidToStringConvertor())
                .addColumnTypeConverter(DurationToLongConvertor())
                .addColumnTypeConverter(InstantToLongConvertor())
                .addColumnTypeConverter(ListToStringConvertor())
                .build()

            // if db is not set the db
            val setDb = databaseRef.compareAndSet(null, instance)
            if (!setDb) return databaseRef.value!!
            return instance
        }
    }
}
