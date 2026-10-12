package com.sam.talkdraft.feature_recordings.data.mapper

import com.sam.talkdraft.database.entities.RecordingEntity
import com.sam.talkdraft.database.entities.TranScriptEntity
import com.sam.talkdraft.database.entities.TranScriptSegmentsEntity
import com.sam.talkdraft.database.relations.RecordingWithTranscript
import com.sam.talkdraft.feature_recordings.domain.model.RecordingModel
import com.sam.talkdraft.feature_recordings.domain.model.RecordingTranscriptSegment
import com.sam.talkdraft.feature_recordings.domain.model.RecordingsMetadataModel
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal fun TranScriptSegmentsEntity.toDomain(): RecordingTranscriptSegment {
    return RecordingTranscriptSegment(
        startDuration = startDuration,
        endDuration = endDuration,
        segment = text,
    )
}

internal fun RecordingWithTranscript.toDomain(
    transcriptionModel: TranscriptionModel? = null,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): RecordingModel {
    val transcriptEntity = transcriptWithSegments?.transcript
    val segmentEntities = transcriptWithSegments?.segments ?: emptyList()

    return RecordingModel(
        id = recording.id,
        metadata = RecordingsMetadataModel(
            title = recording.title,
            audioPath = recording.audioPath,
            totalDuration = recording.duration,
            createdAt = recording.createdAt.toLocalDateTime(timeZone),
            updatedAt = recording.updatedAt.toLocalDateTime(timeZone),
            isPinned = recording.isPinned,
            isFavourite = recording.isFavourite,
            language = transcriptEntity?.languageCode ?: "en",
        ),
        fullTranscription = transcriptEntity?.text ?: "",
        segments = segmentEntities.map { it.toDomain() },
        transcriptionModel = transcriptionModel,
    )
}

fun RecordingEntity.toDomain(
    transcript: TranScriptEntity? = null,
    segments: List<TranScriptSegmentsEntity> = emptyList(),
    transcriptionModel: TranscriptionModel? = null,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): RecordingModel {
    return RecordingModel(
        id = id,
        metadata = RecordingsMetadataModel(
            title = title,
            audioPath = audioPath,
            totalDuration = duration,
            createdAt = createdAt.toLocalDateTime(timeZone),
            updatedAt = updatedAt.toLocalDateTime(timeZone),
            isPinned = isPinned,
            isFavourite = isFavourite,
            language = transcript?.languageCode ?: "en",
        ),
        fullTranscription = transcript?.text ?: "",
        segments = segments.map { it.toDomain() },
        transcriptionModel = transcriptionModel,
    )
}
