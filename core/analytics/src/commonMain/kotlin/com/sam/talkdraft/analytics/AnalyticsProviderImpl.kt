package com.sam.talkdraft.analytics

import co.touchlab.kermit.Logger
import com.posthog.kmp.PostHog
import org.koin.core.annotation.Singleton

@Singleton(binds = [IAnalyticsProvider::class])
internal class AnalyticsProviderImpl : IAnalyticsProvider {

    override fun identify(userId: String, properties: Map<String, Any>) {
        Logger.d(tag = TAG) { "SET DEVICE IDENTITY" }
        PostHog.identify(distinctId = userId, userProperties = properties)
    }

    override fun track(event: AnalyticsEvent, properties: Map<String, Any>) {
        Logger.d(tag = TAG) { "CAPTURE EVENT :$event" }
        PostHog.capture(event = event.name, properties = properties)
    }

    override fun reset() {
        Logger.d(tag = TAG) { "RESET ANALYTICS" }
        PostHog.reset()
    }

    companion object {
        private const val TAG = "AnalyticsProvider"
    }
}
