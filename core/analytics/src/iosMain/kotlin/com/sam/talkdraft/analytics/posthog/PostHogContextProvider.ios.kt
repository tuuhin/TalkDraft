package com.sam.talkdraft.analytics.posthog

import com.posthog.kmp.PostHogContext
import org.koin.core.annotation.Singleton

@Singleton
internal actual class PostHogContextProvider {

	actual fun readContext(): PostHogContext = PostHogContext()
}