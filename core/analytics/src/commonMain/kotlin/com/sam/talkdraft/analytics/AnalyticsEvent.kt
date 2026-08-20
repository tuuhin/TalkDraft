package com.sam.talkdraft.analytics

sealed class AnalyticsEvent(val name: String) {
	data object AppOpened : AnalyticsEvent("app_open")
	data object UserSignedIn : AnalyticsEvent("user_sign_in")
	data object UserSignOut : AnalyticsEvent("user_sign_out")
	data object OnboardingCompleted : AnalyticsEvent("onboarding_complete")
	data object ModelDownloadStarted : AnalyticsEvent("model_download_started")
	data object ModelDownloadCompleted : AnalyticsEvent("model_download_completed")
}