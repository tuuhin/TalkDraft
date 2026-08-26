package com.sam.talkdraft.workers

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkerFactory
import androidx.work.testing.TestDriver
import androidx.work.testing.WorkManagerTestInitHelper
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.sam.talkdraft.model_downloader.domain.models.ModelDownloadStatus
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import com.sam.talkdraft.testing.annotations.RunWithPlatform
import com.sam.talkdraft.workers.di.AndroidWorkerTestModule
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.koin.plugin.module.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.inject

@RunWithPlatform
class ModelDownloadWorkerTest : KoinTest {

    private val context by inject<Context>()
    private val registrar by inject<IModelDownloadRegistrar>()
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

    @Test
    fun test_download_model_worker() = runTest {

        val req = registrar.startModelDownload(model)
        val workId = req.toJavaUuid()

        testDriver.setAllConstraintsMet(workId)
        testDriver.setInitialDelayMet(workId)

        registrar.observerDownloadStatus(req)
            .test(5.seconds) {
                val item = awaitItem()
                assertThat(item).isEqualTo(ModelDownloadStatus.DownloadInitiated)

                val item2 = awaitItem()
                assertThat(item2).isInstanceOf(ModelDownloadStatus.Downloading::class)

                cancelAndIgnoreRemainingEvents()
            }
    }
}
