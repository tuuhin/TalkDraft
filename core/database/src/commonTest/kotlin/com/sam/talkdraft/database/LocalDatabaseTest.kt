package com.sam.talkdraft.database

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.sam.talkdraft.database.dao.GeneratedNotesDao
import com.sam.talkdraft.database.dao.LocalTranscriptModelsDao
import com.sam.talkdraft.database.dao.ProcessingJobDao
import com.sam.talkdraft.database.dao.RecordingDao
import com.sam.talkdraft.database.dao.TranscriptSegmentsDao
import com.sam.talkdraft.database.dao.TranscriptsDao
import com.sam.talkdraft.database.entities.RecordingEntity
import com.sam.talkdraft.database.utils.AppDBBuilder
import com.sam.talkdraft.testing.annotations.RunWithPlatform
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

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
    private lateinit var transcriptModelDao: LocalTranscriptModelsDao

    @BeforeTest
    fun setup() {
        database = builder.getMemoryDbBuilder().build()
        recordingDao = database.recordingsDao()
        transcriptDao = database.transcriptsDao()
        transcriptSegmentDao = database.transcriptsSegmentDao()
        generatedNoteDao = database.generatedNotesDao()
        processingJobDao = database.processingDao()
        transcriptModelDao = database.transcriptsModelDao()
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

        assertThat(result).isNotNull()
            .isEqualTo(recording)


        recordingDao.setFavourite(
            id = recording.id,
            isFavourite = true,
            updatedAt = Clock.System.now(),
        )

        val updated = recordingDao.getById(recording.id)

        assertThat(updated?.isFavourite).isNotNull()
            .isTrue()

        recordingDao.deleteById(recording.id)
        assertThat(recordingDao.getById(recording.id)).isNull()
    }
}
