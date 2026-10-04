package com.sam.talkdraft.feature_recordings.domain.model

import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import kotlin.uuid.Uuid

data class RecordingModel(
    val id: Uuid,
    val metadata: RecordingsMetadataModel,
    val fullTranscription: String = "",
    val segments: List<RecordingTranscriptSegment> = emptyList(),
    val transcriptionModel: TranscriptionModel? = null,
)
