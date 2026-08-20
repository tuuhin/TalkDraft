package com.sam.talkdraft.database.relations

import androidx.room3.Embedded
import androidx.room3.Relation
import com.sam.talkdraft.database.entities.TranScriptEntity
import com.sam.talkdraft.database.entities.TranScriptSegmentsEntity

data class TranscriptWithSegments(
    @Embedded
    val transcript: TranScriptEntity,

    @Relation(
        parentColumns = ["_id"],
        entityColumns = ["transcript_id"],
    )
    val segments: List<TranScriptSegmentsEntity>
)
