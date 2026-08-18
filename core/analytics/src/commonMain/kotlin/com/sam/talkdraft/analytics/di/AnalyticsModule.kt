package com.sam.talkdraft.analytics.di

import com.posthog.kmp.PostHog
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
import org.koin.core.annotation.Singleton

@Module
@ComponentScan("com.sam.talkdraft.analytics")
class AnalyticsModule {

	@Singleton
	internal fun providesPostHog(): PostHog = PostHog
}