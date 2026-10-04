package com.sam.talkdraft.feature_recordings.domain.model

data class CreateRecordingModel(
    val audioFilePath: String,
    val transcriptions: List<RecordingTranscriptSegment>,
)
