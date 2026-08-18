package com.sam.talkdraft.di

import com.sam.talkdraft.analytics.di.AnalyticsModule
import com.sam.talkdraft.crashlytics.di.CrashlyticsModule
import org.koin.core.annotation.KoinApplication

@KoinApplication(
	modules = [
		AnalyticsModule::class,
		CrashlyticsModule::class,
	]
)
internal class KoinTalkDraftApp