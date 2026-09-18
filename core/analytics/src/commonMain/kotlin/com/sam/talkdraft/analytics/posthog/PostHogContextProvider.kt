package com.sam.talkdraft.analytics.posthog

import com.posthog.kmp.PostHogContext
import org.koin.core.annotation.Singleton

@Singleton(binds = [IPostHogContext::class])
internal expect class PostHogContextProvider : IPostHogContext {

    override fun readContext(): PostHogContext
}
