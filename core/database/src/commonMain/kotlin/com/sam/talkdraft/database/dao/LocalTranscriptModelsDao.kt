package com.sam.talkdraft.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.sam.talkdraft.database.entities.LocalTranscriptModelEntity
import com.sam.talkdraft.database.enums.ModelDownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Dao
interface LocalTranscriptModelsDao {

    @Upsert
    suspend fun upsert(model: LocalTranscriptModelEntity)

    @Query("SELECT * FROM transcript_models WHERE _id = :id LIMIT 1")
    suspend fun getById(id: Uuid): LocalTranscriptModelEntity?

    @Query(" SELECT * FROM transcript_models WHERE status = :status")
    suspend fun getByStatus(status: ModelDownloadStatus): List<LocalTranscriptModelEntity>

    @Query("SELECT * FROM transcript_models WHERE status = :status ORDER BY downloaded_at DESC")
    fun observeInstalledModels(status: ModelDownloadStatus = ModelDownloadStatus.DOWNLOADED)
        : Flow<List<LocalTranscriptModelEntity>>

    @Query("DELETE FROM transcript_models WHERE _id = :id")
    suspend fun deleteById(id: Uuid)
}
