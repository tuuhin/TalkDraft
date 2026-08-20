package com.sam.talkdraft.app

import com.sam.talkdraft.analytics.di.AnalyticsModule
import com.sam.talkdraft.auth.di.AuthModule
import com.sam.talkdraft.common.di.CommonModule
import com.sam.talkdraft.crashlytics.di.CrashlyticsModule
import com.sam.talkdraft.supabase.di.SupabaseModule
import org.koin.core.annotation.KoinApplication

@KoinApplication(
	modules = [
		CommonModule::class,
		AuthModule::class,
		AnalyticsModule::class,
		CrashlyticsModule::class,
		SupabaseModule::class,
//		DBModule::class,
	]
)
class KoinTalkDraftApp