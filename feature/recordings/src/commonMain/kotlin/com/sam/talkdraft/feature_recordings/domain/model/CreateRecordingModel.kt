package com.sam.talkdraft.feature_recordings.domain.model

import kotlin.uuid.Uuid

data class CreateRecordingModel(
    val modelId: Uuid,
    val audioFilePath: String,
    val transcriptions: List<RecordingTranscriptSegment>,
    val fullText: String = "",
    val title: String? = null,
)
