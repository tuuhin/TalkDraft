package com.sam.talkdraft.analytics.posthog

import android.app.Application
import com.posthog.kmp.PostHogContext
import org.koin.core.annotation.Singleton

@Singleton
internal actual class PostHogContextProvider(private val app: Application) {

    actual fun readContext(): PostHogContext = PostHogContext(app)
}
