package com.sam.talkdraft.database.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.sam.talkdraft.database.entities.RecordingEntity
import com.sam.talkdraft.database.entities.TranScriptEntity
import com.sam.talkdraft.database.entities.TranScriptSegmentsEntity
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    @Upsert
    suspend fun updateOrInsertRecordingEntry(recording: RecordingEntity)

    @Upsert
    suspend fun upsertOrInsertTranscript(transcript: TranScriptEntity)

    @Insert
    suspend fun insertTranscriptionSegments(segments: List<TranScriptSegmentsEntity>)

    @Delete
    suspend fun delete(recording: RecordingEntity)

    @Query("SELECT * FROM recordings_table WHERE _id = :id LIMIT 1")
    suspend fun getById(id: Uuid): RecordingEntity?

    @Query("SELECT * FROM recordings_table ORDER BY created_at DESC")
    fun observeAll(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings_table WHERE is_favourite = 1 ORDER BY created_at DESC")
    fun observeFavourites(): Flow<List<RecordingEntity>>

    @Query("UPDATE recordings_table SET is_favourite = :isFavourite, updated_at = :updatedAt WHERE _id = :id")
    suspend fun setFavourite(id: Uuid, isFavourite: Boolean, updatedAt: Instant)

    @Query("UPDATE recordings_table SET is_pinned = :isPinned, updated_at = :updatedAt WHERE _id = :id")
    suspend fun setPinned(id: Uuid, isPinned: Boolean, updatedAt: Instant)

    @Query("DELETE FROM recordings_table WHERE _id = :id")
    suspend fun deleteById(id: Uuid)

    @Transaction
    suspend fun insertFullRecording(
        recording: RecordingEntity,
        transcript: TranScriptEntity,
        segments: List<TranScriptSegmentsEntity>,
    ) {
        updateOrInsertRecordingEntry(recording)
        upsertOrInsertTranscript(transcript)
        if (segments.isNotEmpty()) insertTranscriptionSegments(segments)
    }
}
