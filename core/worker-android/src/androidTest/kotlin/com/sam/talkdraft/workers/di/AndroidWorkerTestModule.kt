package com.sam.talkdraft.workers.di

import androidx.work.WorkerFactory
import com.sam.talkdraft.analytics.IAnalyticsProvider
import com.sam.talkdraft.model_downloader.domain.IModelDownloadManager
import com.sam.talkdraft.testing.di.TestPlatformModule
import com.sam.talkdraft.workers.fakes.TestAnalyticsProvider
import com.sam.talkdraft.workers.fakes.TestModelDownloadManager
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module(
    includes = [
        TestPlatformModule::class,
        AndroidWorkersModule::class,
    ],
)
class AndroidWorkerTestModule {

    @Singleton
    fun providesFakeDownloader(): IModelDownloadManager = TestModelDownloadManager()

    @Singleton
    fun provideFakeAnalyticsProvider(): IAnalyticsProvider = TestAnalyticsProvider()

    @Singleton
    fun providesWorkerFactory(): WorkerFactory = KoinWorkerFactory()
}
