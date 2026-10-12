package com.sam.talkdraft.feature_recordings.data

import com.sam.talkdraft.common.utils.Resource
import com.sam.talkdraft.database.dao.RecordingDao
import com.sam.talkdraft.database.dao.RecordingsDetailsDao
import com.sam.talkdraft.database.dao.TranscriptsDao
import com.sam.talkdraft.database.entities.RecordingEntity
import com.sam.talkdraft.database.entities.TranScriptEntity
import com.sam.talkdraft.database.relations.RecordingWithTranscript
import com.sam.talkdraft.feature_recordings.data.mapper.toDomain
import com.sam.talkdraft.feature_recordings.data.mapper.toEntities
import com.sam.talkdraft.feature_recordings.domain.IRecordingFileManager
import com.sam.talkdraft.feature_recordings.domain.IRecordingsRepository
import com.sam.talkdraft.feature_recordings.domain.InvalidTranscriptionIdException
import com.sam.talkdraft.feature_recordings.domain.exception.InvalidRecordingIdException
import com.sam.talkdraft.feature_recordings.domain.model.CreateRecordingModel
import com.sam.talkdraft.feature_recordings.domain.model.RecordingModel
import com.sam.talkdraft.player.IAudioMetadataExtractor
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.datetime.TimeZone
import okio.Path.Companion.toPath
import org.koin.core.annotation.Factory

@Factory(binds = [IRecordingsRepository::class])
internal class RecordingRepositoryImpl(
    private val recordingsDao: RecordingDao,
    private val recordingsDetailsDao: RecordingsDetailsDao,
    private val transcriptsDao: TranscriptsDao,
    private val fileManager: IRecordingFileManager,
    private val metadataExtractor: IAudioMetadataExtractor,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : IRecordingsRepository {

    override fun getAllRecordings(): Flow<Resource<List<RecordingModel>, Exception>> {
        return recordingsDetailsDao.observeAllDetails()
            .map<List<RecordingWithTranscript>, Resource<List<RecordingModel>, Exception>> { list ->
                val models = list.map { detail -> detail.toDomain(timeZone = timeZone) }
                Resource.Success(models)
            }
            .catch { err ->
                if (err is Exception) emit(Resource.Error(err, err.message))
            }.onStart {
                emit(Resource.Loading)
            }
    }

    override suspend fun saveNewRecording(model: CreateRecordingModel): Result<RecordingModel> {
        return try {
            // prepare all the metadata
            val recordingId = Uuid.random()
            val transcriptId = Uuid.random()

            // copies the old file path to the new file path and deletes the old file too
            val filePath = fileManager.saveFileContent(recordingId.toHexDashString(), model.audioFilePath.toPath())
                .getOrElse { err -> return Result.failure(err) }

            // extracts the metadata for the file
            val metaData = metadataExtractor.extractMetadataFromAudioFilePath(filePath)
                .getOrElse { err -> return Result.failure(err) }

            val recordingEntity = RecordingEntity(
                id = recordingId,
                title = model.title ?: "Untitled",
                audioPath = filePath.toString(),
                duration = metaData.duration,
            )

            val transcriptEntity = TranScriptEntity(
                id = transcriptId,
                recordingId = recordingId,
                modelId = model.modelId,
                text = model.fullText,
            )

            // inset the entries
            recordingsDao.insertFullRecording(
                recording = recordingEntity,
                transcript = transcriptEntity,
                segments = model.transcriptions.toEntities(transcriptId),
            )

            // fetch all the values now
            val resultRecording = recordingsDao.getById(recordingId) ?: throw InvalidRecordingIdException()
            val resultTranscript = transcriptsDao.getById(transcriptId) ?: throw InvalidTranscriptionIdException()
            val recordingModel =
                resultRecording.toDomain(transcript = resultTranscript.transcript, segments = resultTranscript.segments)

            Result.success(recordingModel)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.failure(e)
        }
    }
}
