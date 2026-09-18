package com.sam.talkdraft.analytics.posthog

import com.posthog.kmp.PostHogContext

internal fun interface IPostHogContext {

    fun readContext(): PostHogContext
}
