package com.sam.talkdraft.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.sam.talkdraft.database.entities.JobProcessingEntity
import com.sam.talkdraft.database.enums.ProcessStatus
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcessingJobDao {

    @Upsert
    suspend fun upsert(job: JobProcessingEntity)

    @Query("SELECT * FROM processing_entries_table WHERE _id = :id LIMIT 1")
    suspend fun getById(id: Uuid): JobProcessingEntity?

    @Query("SELECT * FROM processing_entries_table WHERE recording_id = :recordingId ORDER BY started_at ASC")
    suspend fun getByRecordingId(recordingId: Uuid): List<JobProcessingEntity>

    @Query("SELECT * FROM processing_entries_table WHERE recording_id = :recordingId ORDER BY started_at ASC")
    fun observeByRecordingId(recordingId: Uuid): Flow<List<JobProcessingEntity>>

    @Query("SELECT * FROM processing_entries_table WHERE status = :status ORDER BY started_at ASC")
    suspend fun getByStatus(status: ProcessStatus): List<JobProcessingEntity>

    @Query("DELETE FROM processing_entries_table WHERE recording_id = :recordingId")
    suspend fun deleteByRecordingId(recordingId: Uuid)
}
