package com.sam.talkdraft.database.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.sam.talkdraft.database.enums.ModelDownloadStatus
import com.sam.talkdraft.database.utils.DBConstants
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(tableName = DBConstants.TRANSCRIPT_MODELS_TABLE_NAME)
data class LocalTranscriptModelEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "_id")
    val id: Uuid = Uuid.random(),

    @ColumnInfo(name = "version")
    val version: String,

    @ColumnInfo(name = "model_path")
    val modelPath: String,

    @ColumnInfo(name = "size_in_bytes")
    val sizeInBytes: Long,

    @ColumnInfo(name = "checksum")
    val checkSum: String?,

    @ColumnInfo(name = "status")
    val modelStatus: ModelDownloadStatus = ModelDownloadStatus.UNKNOWN,

    @ColumnInfo("downloaded_at")
    val downloadedAt: Instant? = null,
)
