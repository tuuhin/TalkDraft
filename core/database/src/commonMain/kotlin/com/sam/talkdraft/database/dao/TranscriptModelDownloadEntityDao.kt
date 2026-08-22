package com.sam.talkdraft.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.sam.talkdraft.database.entities.DownloadedTranscriptionModelEntity
import com.sam.talkdraft.database.enums.ModelDownloadStatus
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptModelDownloadEntityDao {

    @Upsert
    suspend fun upsert(model: DownloadedTranscriptionModelEntity)

    @Query("SELECT * FROM downloaded_model_table WHERE _id = :id LIMIT 1")
    suspend fun getById(id: Uuid): DownloadedTranscriptionModelEntity?

    @Query(" SELECT * FROM downloaded_model_table WHERE status = :status")
    suspend fun getByStatus(status: ModelDownloadStatus): List<DownloadedTranscriptionModelEntity>

    @Query("SELECT * FROM downloaded_model_table WHERE status = :status ORDER BY downloaded_at DESC")
    fun observeInstalledModels(status: ModelDownloadStatus = ModelDownloadStatus.DOWNLOADED)
        : Flow<List<DownloadedTranscriptionModelEntity>>

    @Query("DELETE FROM downloaded_model_table WHERE _id = :id")
    suspend fun deleteById(id: Uuid)
}
