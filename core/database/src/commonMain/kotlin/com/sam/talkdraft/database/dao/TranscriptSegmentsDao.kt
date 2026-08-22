package com.sam.talkdraft.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Upsert
import com.sam.talkdraft.database.entities.TranScriptSegmentsEntity
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptSegmentsDao {

    @Upsert
    suspend fun insert(segment: TranScriptSegmentsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(segments: List<TranScriptSegmentsEntity>)

    @Query("SELECT * FROM transcripts_segments_table WHERE transcript_id = :transcriptId ORDER BY start_time ASC")
    suspend fun getByTranscriptId(transcriptId: Uuid): List<TranScriptSegmentsEntity>

    @Query("SELECT * FROM transcripts_segments_table WHERE transcript_id = :transcriptId ORDER BY start_time ASC")
    fun observeByTranscriptId(transcriptId: Uuid): Flow<List<TranScriptSegmentsEntity>>

    @Query("DELETE FROM transcripts_segments_table WHERE transcript_id = :transcriptId")
    suspend fun deleteByTranscriptId(transcriptId: Uuid)

    @Query("DELETE FROM transcripts_segments_table WHERE _id = :id")
    suspend fun deleteById(id: Uuid)
}
