package com.sam.talkdraft.feature_recordings.data.mapper

import com.sam.talkdraft.database.entities.TranScriptSegmentsEntity
import com.sam.talkdraft.feature_recordings.domain.model.RecordingTranscriptSegment
import kotlin.uuid.Uuid

internal fun List<RecordingTranscriptSegment>.toEntities(transcriptId: Uuid): List<TranScriptSegmentsEntity> {
    return map { segment ->
        TranScriptSegmentsEntity(
            id = Uuid.random(),
            transcriptId = transcriptId,
            startDuration = segment.startDuration,
            endDuration = segment.endDuration,
            text = segment.segment,
            speakerId = segment.speakerId?.toHexString(),
        )
    }
}
