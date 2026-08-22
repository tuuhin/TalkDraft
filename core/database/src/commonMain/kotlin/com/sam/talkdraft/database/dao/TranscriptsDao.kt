package com.sam.talkdraft.database.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Query
import androidx.room3.Upsert
import com.sam.talkdraft.database.entities.TranScriptEntity
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptsDao {

    @Upsert
    suspend fun upsert(transcript: TranScriptEntity)

    @Query("SELECT * FROM transcripts_table WHERE _id = :id LIMIT 1")
    suspend fun getById(id: Uuid): TranScriptEntity?

    @Query("SELECT * FROM transcripts_table WHERE recording_id = :recordingId LIMIT 1")
    suspend fun getByRecordingId(recordingId: Uuid): TranScriptEntity?

    @Query("SELECT * FROM transcripts_table WHERE recording_id = :recordingId LIMIT 1")
    fun observeByRecordingId(recordingId: Uuid): Flow<TranScriptEntity?>

    @Delete
    suspend fun delete(transcript: TranScriptEntity)

    @Query("DELETE FROM transcripts_table WHERE recording_id = :recordingId")
    suspend fun deleteByRecordingId(recordingId: Uuid)
}
