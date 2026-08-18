package com.sam.talkdraft.analytics.posthog

import com.posthog.kmp.PostHogContext
import org.koin.core.annotation.Singleton

@Singleton
internal expect class PostHogContextProvider {

	fun readContext(): PostHogContext
}