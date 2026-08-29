package com.sam.talkdraft.database.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.sam.talkdraft.database.entities.DownloadedTranscriptionModelEntity
import com.sam.talkdraft.database.entities.TranscriptionModelEntity
import com.sam.talkdraft.database.enums.ModelDownloadStatus
import com.sam.talkdraft.database.relations.LocalTranscriptionModelWithDownloadInfo
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalTranscriptionEntityDao {

    @Transaction
    @Query("SELECT * FROM transciption_model_table ORDER BY display_name ASC")
    fun observeModels(): Flow<List<LocalTranscriptionModelWithDownloadInfo>>

    @Transaction
    @Query("SELECT * FROM transciption_model_table ORDER BY display_name ASC")
    suspend fun readAllModels(): List<LocalTranscriptionModelWithDownloadInfo>

    @Transaction
    @Query("SELECT * FROM transciption_model_table WHERE total_size_in_bytes <= :maxModelSize ORDER BY total_size_in_bytes ASC LIMIT 1")
    suspend fun readSmallestModel(maxModelSize: Long): LocalTranscriptionModelWithDownloadInfo?

    @Transaction
    @Query("SELECT * FROM transciption_model_table WHERE _id=:id")
    fun getModelFlow(id: Uuid): Flow<LocalTranscriptionModelWithDownloadInfo?>

    @Transaction
    @Query("SELECT * FROM transciption_model_table WHERE _id=:id")
    suspend fun getModel(id: Uuid): LocalTranscriptionModelWithDownloadInfo?

    @Upsert
    suspend fun upsertTranscriptionModels(entities: List<TranscriptionModelEntity>)

    @Transaction
    suspend fun updateDownloadModelStatus(modelId: Uuid, status: ModelDownloadStatus) {
        val existing = readDownloadEntryWithRemoteId(modelId)
        val now = Clock.System.now()

        val entryToSave = existing?.copy(
            modelStatus = status,
            downloadedAt = if (status == ModelDownloadStatus.DOWNLOADED) now else existing.downloadedAt,
        ) ?: DownloadedTranscriptionModelEntity(
            remoteId = modelId,
            modelStatus = status,
            downloadedAt = if (status == ModelDownloadStatus.DOWNLOADED) now else null,
        )

        updateORInsetDownloadEntry(entryToSave)
    }

    @Transaction
    suspend fun updateDownloadModelPath(modelId: Uuid, modelPath: String? = null) {
        val existing = readDownloadEntryWithRemoteId(modelId)

        val entryToSave = existing?.copy(
            modelPath = modelPath,
            modelStatus = if (modelPath == null) ModelDownloadStatus.UNKNOWN else existing.modelStatus,
        ) ?: DownloadedTranscriptionModelEntity(
            remoteId = modelId,
            modelPath = modelPath,
            modelStatus = ModelDownloadStatus.UNKNOWN,
        )

        updateORInsetDownloadEntry(entryToSave)
    }

    @Query("SELECT * from downloaded_model_table where remote_id=:modelId")
    suspend fun readDownloadEntryWithRemoteId(modelId: Uuid): DownloadedTranscriptionModelEntity?

    @Upsert
    suspend fun updateORInsetDownloadEntry(entity: DownloadedTranscriptionModelEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM transciption_model_table LIMIT 1)")
    suspend fun isNotEmpty(): Boolean

    @Delete
    suspend fun deleteTranscriptionsModels(entities: List<TranscriptionModelEntity>)
}
