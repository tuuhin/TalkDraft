package com.sam.talkdraft.workers

import android.content.Context
import androidx.concurrent.futures.await
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.testing.TestDriver
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isSameInstanceAs
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import com.sam.talkdraft.testing.annotations.RunWithPlatform
import com.sam.talkdraft.workers.di.AndroidWorkerTestModule
import com.sam.talkdraft.workers.workers.TranscriptionModuleDownloadWorker
import com.sam.talkdraft.workers.workers.WorkParams
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlin.uuid.Uuid
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.koin.plugin.module.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject

@RunWithPlatform
class ModelDownloadWorkerTest : KoinTest {

    private val context by inject<Context>()
    private val factory by inject<WorkerFactory>()

    private val model = mockk<TranscriptionModel>()

    lateinit var testDriver: TestDriver

    @get:Rule
    val koinRule = KoinTestRule.create {
        module<AndroidWorkerTestModule>()
    }

    @BeforeTest
    fun setup() {
        coEvery { model.id } returns Uuid.random()

        val config = Configuration.Builder()
            .setWorkerFactory(factory)
            .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        testDriver = WorkManagerTestInitHelper.getTestDriver(context)
            ?: error("TestDriver is not available")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun test_download_model_worker() = runTest {
        val inputData = workDataOf(WorkParams.TRANSCRIPTION_MODEL_ID_INPUT_KEY to model.id.toString())

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(false)
            .build()

        val downloadRequest = OneTimeWorkRequestBuilder<TranscriptionModuleDownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .setInitialDelay(2.seconds.toJavaDuration())
            .addTag(TEST_DOWNLOAD_WORK_TAG_PREFIX + model.id)
            .build()

        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(WORK_NAME_PREFIX + model.id, ExistingWorkPolicy.REPLACE, downloadRequest)

        val workId = downloadRequest.id
        testDriver.setAllConstraintsMet(workId)
        testDriver.setInitialDelayMet(workId)

        val workResult = workManager.getWorkInfoById(downloadRequest.id)
            .await()
        advanceUntilIdle()

        assertThat(workResult?.state).isNotNull()
        assertThat(workResult?.state).isSameInstanceAs(WorkInfo.State.RUNNING)

    }

    companion object {
        private const val TEST_DOWNLOAD_WORK_TAG_PREFIX = "test_download_task"
        private const val WORK_NAME_PREFIX = "test_worker"
    }
}
