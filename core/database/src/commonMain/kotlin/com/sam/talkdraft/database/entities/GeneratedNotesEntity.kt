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
    tableName = DBConstants.GENERATED_NOTES_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = RecordingEntity::class,
            parentColumns = ["_id"],
            childColumns = ["recording_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("recording_id", unique = true),
    ],
)
data class GeneratedNotesEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "_id")
    val id: Uuid = Uuid.random(),

    @ColumnInfo("recording_id")
    val recordingId: Uuid,

    @ColumnInfo("title")
    val title: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "content_hash")
    val contentHash: String,

    @ColumnInfo("created_at")
    val createdAt: Instant,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)
