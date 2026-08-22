package com.sam.talkdraft.database.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.sam.talkdraft.database.enums.ModelDownloadStatus
import com.sam.talkdraft.database.utils.DBConstants
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(
    tableName = DBConstants.DOWNLOADED_MODEL_TABLE,
    foreignKeys = [
        ForeignKey(
            entity = TranscriptionModelEntity::class,
            parentColumns = ["_id"],
            childColumns = ["remote_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("remote_id", unique = true)],
)
data class DownloadedTranscriptionModelEntity(

    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "_id")
    val id: Uuid = Uuid.random(),

    @ColumnInfo(name = "remote_id")
    val remoteId: Uuid,

    @ColumnInfo(name = "model_path")
    val modelPath: String? = null,

    @ColumnInfo(name = "status")
    val modelStatus: ModelDownloadStatus = ModelDownloadStatus.UNKNOWN,

    @ColumnInfo(name = "downloaded_at")
    val downloadedAt: Instant? = null,
)
