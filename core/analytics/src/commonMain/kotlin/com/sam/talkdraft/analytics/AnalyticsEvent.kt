package com.sam.talkdraft.analytics

sealed class AnalyticsEvent(val name: String) {
	data object AppOpened : AnalyticsEvent("app_open")
	data object OnboardingCompleted : AnalyticsEvent("onboarding_complete")
	data object ModelDownloadStarted : AnalyticsEvent("model_download_started")
	data object ModelDownloadCompleted : AnalyticsEvent("model_download_completed")
}