package com.sam.talkdraft.onboarding.models

internal sealed interface OnboardingEvents {
    data class OnSkipOnboarding(val screen: OnboardingScene) : OnboardingEvents
    data object OnOnboardingCompleted : OnboardingEvents
    data class OnAddToCaptureItems(val item: CaptureIdeaOption) : OnboardingEvents
    data object RequestPermissions : OnboardingEvents
    data object RequestOpenAppSettings : OnboardingEvents
    data class SendAnalyticsEvent(val screen: OnboardingScene, val extras: Map<String, Any> = emptyMap()) :
        OnboardingEvents
}
