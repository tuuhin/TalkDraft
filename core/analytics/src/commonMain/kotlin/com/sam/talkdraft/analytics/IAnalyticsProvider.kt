package com.sam.talkdraft.analytics

interface IAnalyticsProvider {
	fun track(event: AnalyticsEvent, properties: Map<String, Any> = emptyMap())
	fun identify(userId: String, properties: Map<String, Any> = emptyMap())
	fun reset()

}