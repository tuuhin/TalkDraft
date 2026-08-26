package com.sam.talkdraft.workers.fakes

import co.touchlab.kermit.Logger
import com.sam.talkdraft.analytics.AnalyticsEvent
import com.sam.talkdraft.analytics.IAnalyticsProvider

private const val TAG = "FAKE_ANALYTICS_PROVIDER"

internal class TestAnalyticsProvider : IAnalyticsProvider {
    override fun identify(userId: String, properties: Map<String, Any>) {
        Logger.d(tag = TAG) { "USER ID:$userId PROPERTIES :$properties" }
    }

    override fun track(event: AnalyticsEvent, properties: Map<String, Any>) {
        Logger.d(tag = TAG) { "EVENT:${event.name} PROPERTIES:$properties" }
    }

    override fun reset() = Unit
}
