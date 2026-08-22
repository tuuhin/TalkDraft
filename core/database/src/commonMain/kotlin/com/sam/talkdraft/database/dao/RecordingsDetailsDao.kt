package com.sam.talkdraft.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import com.sam.talkdraft.database.relations.RecordingWithTranscript
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingsDetailsDao {

    @Transaction
    @Query("SELECT * FROM recordings_table WHERE _id = :recordingId LIMIT 1")
    fun observeDetails(recordingId: Uuid): Flow<RecordingWithTranscript?>
}
