package com.sam.talkdraft

import android.app.Application
import com.sam.talkdraft.analytics.posthog.IPostHogInitManager
import com.sam.talkdraft.app.KoinTalkDraftApp
import com.sam.talkdraft.crashlytics.MeasureSetupManager
import com.sam.talkdraft.notifications.INotificationChannelRegistrar
import com.sam.talkdraft.workers.IStartupWorkerRegistrar
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.KoinConfiguration
import org.koin.plugin.module.dsl.koinConfiguration

@OptIn(KoinExperimentalAPI::class)
class TalkDraftApplication : Application(), KoinStartup {

    private val posthogInit by inject<IPostHogInitManager>()
    private val measure by inject<MeasureSetupManager>()
    private val notificationRegistrar by inject<INotificationChannelRegistrar>()
    private val workerRegistrar by inject<IStartupWorkerRegistrar>()

    override fun onCreate() {
        super.onCreate()
        if (!BuildConfig.DEBUG) {
            // only set in release mode
            posthogInit.setup()
            measure.setup()
        }
        notificationRegistrar.registerChannels()
        workerRegistrar.enqueueWorkers()
    }

    override fun onKoinStartup(): KoinConfiguration = koinConfiguration<KoinTalkDraftApp> {
        androidContext(this@TalkDraftApplication)
        androidLogger()
        workManagerFactory()
    }
}
