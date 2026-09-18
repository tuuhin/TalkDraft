package com.sam.talkdraft.analytics.posthog

import com.posthog.kmp.PostHogContext
import org.koin.core.annotation.Singleton

@Singleton(binds = [IPostHogContext::class])
internal actual class PostHogContextProvider : IPostHogContext {

    actual override fun readContext(): PostHogContext = PostHogContext()
}
