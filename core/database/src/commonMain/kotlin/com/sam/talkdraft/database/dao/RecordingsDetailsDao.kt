package com.sam.talkdraft.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import com.sam.talkdraft.database.relations.RecordingWithTranscript
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.Uuid

@Dao
interface RecordingsDetailsDao {

    @Transaction
    @Query("SELECT * FROM recordings WHERE _id = :recordingId LIMIT 1")
    fun observeDetails(recordingId: Uuid): Flow<RecordingWithTranscript?>
}
