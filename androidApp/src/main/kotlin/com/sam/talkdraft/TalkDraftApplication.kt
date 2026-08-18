package com.sam.talkdraft

import android.app.Application
import com.sam.talkdraft.analytics.posthog.IPostHogInitManager
import com.sam.talkdraft.di.KoinTalkDraftApp
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.KoinConfiguration
import org.koin.plugin.module.dsl.koinConfiguration

@OptIn(KoinExperimentalAPI::class)
class TalkDraftApplication : Application(), KoinStartup {

	private val posthogInit by inject<IPostHogInitManager>()

	override fun onCreate() {
		posthogInit.setup()
		super.onCreate()
	}

	override fun onKoinStartup(): KoinConfiguration = koinConfiguration<KoinTalkDraftApp> {
		androidContext(this@TalkDraftApplication)
		androidLogger()
	}
}