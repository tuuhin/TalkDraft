package com.sam.talkdraft.database
import com.sam.talkdraft.database.dao.GeneratedNotesDao
import com.sam.talkdraft.database.dao.ProcessingJobDao
import com.sam.talkdraft.database.dao.RecordingDao
import com.sam.talkdraft.database.dao.TranscriptModelDownloadEntityDao
import com.sam.talkdraft.database.dao.TranscriptSegmentsDao
import com.sam.talkdraft.database.dao.TranscriptsDao
import com.sam.talkdraft.database.entities.DownloadedTranscriptionModelEntity
import com.sam.talkdraft.database.entities.RecordingEntity
import com.sam.talkdraft.database.entities.TranscriptionModelEntity
import com.sam.talkdraft.database.enums.DBModelDownloadStatus
import com.sam.talkdraft.database.enums.DBModelFamilyOption
import com.sam.talkdraft.database.enums.DBRemoteModelStatus
import com.sam.talkdraft.database.utils.AppDBBuilder
import com.sam.talkdraft.testing.annotations.RunWithPlatform
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.test.KoinTest
import org.koin.test.inject

@OptIn(ExperimentalCoroutinesApi::class)
@RunWithPlatform
class LocalDatabaseTest : KoinTest {

    private val builder by inject<AppDBBuilder>()

    private lateinit var database: TalkDraftDB

    private lateinit var recordingDao: RecordingDao
    private lateinit var transcriptDao: TranscriptsDao
    private lateinit var transcriptSegmentDao: TranscriptSegmentsDao
    private lateinit var generatedNoteDao: GeneratedNotesDao
    private lateinit var processingJobDao: ProcessingJobDao
    private lateinit var transcriptModelDao: TranscriptModelDownloadEntityDao

    @BeforeTest
    fun setup() {
        database = builder.getMemoryDbBuilder().build()
        recordingDao = database.recordingsDao()
        transcriptDao = database.transcriptsDao()
        transcriptSegmentDao = database.transcriptsSegmentDao()
        generatedNoteDao = database.generatedNotesDao()
        processingJobDao = database.processingDao()
        transcriptModelDao = database.downloadEntityDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun recording_crud_works() = runTest {
        val recording = RecordingEntity(
            id = Uuid.random(),
            title = "Test Recording",
            audioPath = "recordings/test.m4a",
            duration = 30.seconds,
            isPinned = false,
            isFavourite = false,
            createdAt = Clock.System.now(),
            updatedAt = Clock.System.now(),
        )

        recordingDao.upsert(recording)
        advanceUntilIdle()

        val result = recordingDao.getById(recording.id)
        advanceUntilIdle()

        assertNotNull(result)
        assertEquals(recording, result)

        recordingDao.setFavourite(
            id = recording.id,
            isFavourite = true,
            updatedAt = Clock.System.now(),
        )

        val updated = recordingDao.getById(recording.id)

        assertNotNull(updated)
        assertTrue(updated.isFavourite)

        recordingDao.deleteById(recording.id)
        assertNull(recordingDao.getById(recording.id))
    }

    @Test
    fun enum_type_converters_and_storage_work() = runTest {
        val modelId = Uuid.random()
        val transcriptionModel = TranscriptionModelEntity(
            id = modelId,
            modelFamily = DBModelFamilyOption.WHISPER,
            variant = "tiny",
            version = "1.0",
            displayName = "Whisper Tiny",
            source = "local",
            repository = "repo",
            revision = "rev",
            artifactPath = "path",
            languages = listOf("en"),
            sizeInBytes = 100L,
            checksum = null,
            status = DBRemoteModelStatus.ACTIVE,
            cachedAt = Clock.System.now(),
            lastSync = Clock.System.now(),
        )
        database.transcriptionModelDao().upsertTranscriptionModels(listOf(transcriptionModel))
        advanceUntilIdle()

        val downloadEntity = DownloadedTranscriptionModelEntity(
            remoteId = modelId,
            modelStatus = DBModelDownloadStatus.DOWNLOADED,
            modelPath = "local/path",
            downloadedAt = Clock.System.now(),
        )
        transcriptModelDao.upsert(downloadEntity)
        advanceUntilIdle()

        val fetched = transcriptModelDao.getById(modelId)
        assertNotNull(fetched)
        assertEquals(DBModelDownloadStatus.DOWNLOADED, fetched.modelStatus)
    }
}
