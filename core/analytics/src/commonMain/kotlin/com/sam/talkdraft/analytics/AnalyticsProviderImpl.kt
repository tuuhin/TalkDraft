package com.sam.talkdraft.analytics

import co.touchlab.kermit.Logger
import com.posthog.kmp.PostHog
import org.koin.core.annotation.Singleton

private const val TAG = "AnalyticsProvider"

@Singleton(binds = [IAnalyticsProvider::class])
internal class AnalyticsProviderImpl(val posthog: PostHog) : IAnalyticsProvider {

	override fun identify(userId: String, properties: Map<String, Any>) {
		Logger.d(tag = TAG) { "SET DEVICE IDENTITY" }
		posthog.identify(distinctId = userId, userProperties = properties)
	}

	override fun track(event: AnalyticsEvent, properties: Map<String, Any>) {
		Logger.d(tag = TAG) { "CAPTURE EVENT :$event" }
		posthog.capture(event = event.name, properties = properties)
	}

	override fun reset() {
		Logger.d(tag = TAG) { "RESET ANALYTICS" }
		posthog.reset()
	}

}