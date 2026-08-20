package com.sam.talkdraft.database.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.sam.talkdraft.database.utils.DBConstants
import kotlin.time.Duration
import kotlin.uuid.Uuid

@Entity(
    tableName = DBConstants.TRANSCRIPT_SEGMENTS_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = TranScriptEntity::class,
            parentColumns = ["_id"],
            childColumns = ["transcript_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("transcript_id"),
    ],
)
data class TranScriptSegmentsEntity(
    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "_id")
    val id: Uuid = Uuid.random(),

    @ColumnInfo(name = "transcript_id")
    val transcriptId: Uuid,

    @ColumnInfo(name = "start_time")
    val startDuration: Duration,

    @ColumnInfo(name = "end_time")
    val endDuration: Duration,

    @ColumnInfo("transcript_text")
    val text: String,

    @ColumnInfo(name = "speaker_id")
    val speakerId: String? = null
)
