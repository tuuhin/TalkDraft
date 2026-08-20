package com.sam.talkdraft.database.relations

import androidx.room3.Embedded
import androidx.room3.Relation
import com.sam.talkdraft.database.entities.GeneratedNotesEntity
import com.sam.talkdraft.database.entities.RecordingEntity

data class RecordingWithGeneratedNote(
    @Embedded
    val recording: RecordingEntity,

    @Relation(
        parentColumns = ["_id"],
        entityColumns = ["recording_id"],
    )
    val note: GeneratedNotesEntity?
)
