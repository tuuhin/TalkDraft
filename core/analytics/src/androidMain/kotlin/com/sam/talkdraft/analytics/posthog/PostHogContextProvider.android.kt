package com.sam.talkdraft.analytics.posthog

import android.app.Application
import com.posthog.kmp.PostHogContext
import org.koin.core.annotation.Singleton

@Singleton(binds = [IPostHogContext::class])
internal actual class PostHogContextProvider(private val app: Application) : IPostHogContext {

    actual override fun readContext(): PostHogContext = PostHogContext(app)
}
