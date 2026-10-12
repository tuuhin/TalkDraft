package com.sam.talkdraft.feature_recorder.models

import com.sam.talkdraft.feature_recordings.domain.model.RecordingTranscriptSegment
import com.sam.talkdraft.transcription.domain.model.TranscriptionSegmentModel
import kotlin.time.Duration

internal data class CompleteRecordingSegments(
    val segment: TranscriptionSegmentModel,
    val start: Duration,
    val end: Duration,
) {
    fun toRecordingEnty(): RecordingTranscriptSegment {
        return RecordingTranscriptSegment(segment = segment.text, startDuration = start, endDuration = end)
    }
}
