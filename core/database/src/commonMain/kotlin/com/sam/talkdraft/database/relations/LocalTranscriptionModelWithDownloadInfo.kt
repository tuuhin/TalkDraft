package com.sam.talkdraft.database.relations

import androidx.room3.Embedded
import androidx.room3.Relation
import com.sam.talkdraft.database.entities.DownloadedTranscriptionModelEntity
import com.sam.talkdraft.database.entities.TranscriptionModelEntity

data class LocalTranscriptionModelWithDownloadInfo(
    @Embedded
    val metaData: TranscriptionModelEntity,

    @Relation(
        parentColumns = ["_id"],
        entityColumns = ["remote_id"],
    )
    val downloadState: DownloadedTranscriptionModelEntity?,
)
