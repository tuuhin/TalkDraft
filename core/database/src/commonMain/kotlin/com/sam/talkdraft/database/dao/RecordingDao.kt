package com.sam.talkdraft.database.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Query
import androidx.room3.Upsert
import com.sam.talkdraft.database.entities.RecordingEntity
import kotlinx.coroutines.flow.Flow
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Dao
interface RecordingDao {

    @Upsert
    suspend fun upsert(recording: RecordingEntity)

    @Delete
    suspend fun delete(recording: RecordingEntity)

    @Query("SELECT * FROM recordings WHERE _id = :id LIMIT 1")
    suspend fun getById(id: Uuid): RecordingEntity?

    @Query("SELECT * FROM recordings ORDER BY created_at DESC")
    fun observeAll(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE is_favourite = 1 ORDER BY created_at DESC")
    fun observeFavourites(): Flow<List<RecordingEntity>>

    @Query("UPDATE recordings SET is_favourite = :isFavourite, updated_at = :updatedAt WHERE _id = :id")
    suspend fun setFavourite(id: Uuid, isFavourite: Boolean, updatedAt: Instant)

    @Query("UPDATE recordings SET is_pinned = :isPinned, updated_at = :updatedAt WHERE _id = :id")
    suspend fun setPinned(id: Uuid, isPinned: Boolean, updatedAt: Instant)

    @Query("DELETE FROM recordings WHERE _id = :id")
    suspend fun deleteById(id: Uuid)
}
