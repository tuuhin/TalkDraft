package com.sam.talkdraft.database.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.sam.talkdraft.database.enums.DBModelArtifactType
import com.sam.talkdraft.database.enums.DBModelFamilyOption
import com.sam.talkdraft.database.enums.DBModelTranscriptionType
import com.sam.talkdraft.database.enums.DBRemoteModelStatus
import com.sam.talkdraft.database.utils.DBConstants
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Entity(tableName = DBConstants.TRANSCRIPTION_MODEL_TABLE)
data class TranscriptionModelEntity(

    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = "_id")
    val id: Uuid,

    @ColumnInfo(name = "model_family")
    val modelFamily: DBModelFamilyOption,

    @ColumnInfo(name = "remote_variant")
    val variant: String,

    @ColumnInfo(name = "remote_version")
    val version: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "remote_source")
    val source: String,

    @ColumnInfo(name = "remote_repository")
    val repository: String,

    @ColumnInfo(name = "remote_commit_hash")
    val revision: String,

    @ColumnInfo(name = "artifact_path")
    val artifactPath: String,

    @ColumnInfo(name = "supported_languages")
    val languages: List<String>,

    @ColumnInfo(name = "total_size_in_bytes")
    val sizeInBytes: Long,

    @ColumnInfo(name = "model_checksum")
    val checksum: String?,

    @ColumnInfo(name = "model_status")
    val status: DBRemoteModelStatus,

    @ColumnInfo(name = "cached_at")
    val cachedAt: Instant,

    @ColumnInfo(name = "last_sync")
    val lastSync: Instant,

    @ColumnInfo(name = "transcription_type", defaultValue = "BATCHED")
    val transcriptionType: DBModelTranscriptionType = DBModelTranscriptionType.BATCHED,

    @ColumnInfo(name = "is_default", defaultValue = "false")
    val isDefault: Boolean = false,

    @ColumnInfo(name = "artifact_type", defaultValue = "BIN")
    val artifactType: DBModelArtifactType = DBModelArtifactType.BINARY,

    @ColumnInfo(name = "description")
    val description: String? = null,
)
