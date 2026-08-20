package com.sam.talkdraft.database.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.sam.talkdraft.database.utils.DBConstants
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(
    tableName = DBConstants.TRANSCRIPT_TABLE_NAME,
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
            onDelete = ForeignKey.SET_DEFAULT,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("recording_id", unique = true),
        Index("model_id"),
    ],
)
data class TranScriptEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "_id")
    val id: Uuid = Uuid.random(),

    @ColumnInfo(name = "recording_id")
    val recordingId: Uuid,

    @ColumnInfo(name = "language_code")
    val languageCode: String = "en-US",

    @ColumnInfo(name = "model_id")
    val modelId: Uuid? = null,

    @ColumnInfo(name = "text")
    val text: String,

    @ColumnInfo("created_at")
    val createdAt: Instant,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)
