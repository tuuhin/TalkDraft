package com.sam.talkdraft.database.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.sam.talkdraft.database.enums.ProcessStatus
import com.sam.talkdraft.database.enums.ProcessingType
import com.sam.talkdraft.database.utils.DBConstants
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(
    tableName = DBConstants.PROCESSING_ENTRIES_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = RecordingEntity::class,
            parentColumns = ["_id"],
            childColumns = ["recording_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LocalTranscriptModelEntity::class,
            parentColumns = ["_id"],
            childColumns = ["model_id"],
            onDelete = ForeignKey.SET_NULL,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("recording_id"),
        Index("model_id"),
    ],
)
data class JobProcessingEntity(

    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "_id")
    val id: Uuid = Uuid.random(),

    @ColumnInfo("recording_id")
    val recordingId: Uuid,

    @ColumnInfo("model_id")
    val modelId: Uuid? = null,

    @ColumnInfo(name = "status")
    val status: ProcessStatus = ProcessStatus.UNKNOWN,

    @ColumnInfo(name = "type")
    val type: ProcessingType = ProcessingType.ON_DEVICE_TRANSCRIPTION,

    @ColumnInfo(name = "error")
    val error: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant = Clock.System.now(),

    @ColumnInfo(name = "started_at")
    val startedAt: Instant,

    @ColumnInfo(name = "finished_at")
    val finishedAt: Instant,
)
