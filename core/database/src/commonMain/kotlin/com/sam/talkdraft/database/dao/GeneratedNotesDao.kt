package com.sam.talkdraft.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import com.sam.talkdraft.database.entities.GeneratedNotesEntity
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Dao
interface GeneratedNotesDao {

    @Upsert
    suspend fun upsert(note: GeneratedNotesEntity)

    @Query("SELECT * FROM generated_notes WHERE _id = :id LIMIT 1")
    suspend fun getById(id: Uuid): GeneratedNotesEntity?

    @Query("SELECT * FROM generated_notes WHERE recording_id = :recordingId LIMIT 1")
    suspend fun getByRecordingId(recordingId: Uuid): GeneratedNotesEntity?

    @Query("SELECT * FROM generated_notes WHERE recording_id = :recordingId LIMIT 1")
    fun observeByRecordingId(recordingId: Uuid): Flow<GeneratedNotesEntity?>

    @Query("DELETE FROM generated_notes WHERE recording_id = :recordingId")
    suspend fun deleteByRecordingId(recordingId: Uuid)
}
