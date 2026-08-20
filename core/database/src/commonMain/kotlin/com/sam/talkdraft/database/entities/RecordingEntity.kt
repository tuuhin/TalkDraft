package com.sam.talkdraft.database.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.sam.talkdraft.database.utils.DBConstants
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(tableName = DBConstants.RECORDING_TABLE_NAME)
data class RecordingEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "_id")
    val id: Uuid = Uuid.random(),

    @ColumnInfo("title")
    val title: String,

    @ColumnInfo("audio_path")
    val audioPath: String,

    @ColumnInfo(name = "duration_ms")
    val duration: Duration,

    @ColumnInfo("is_pinned")
    val isPinned: Boolean = false,

    @ColumnInfo("is_favourite")
    val isFavourite: Boolean = false,

    @ColumnInfo("created_at")
    val createdAt: Instant,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)
