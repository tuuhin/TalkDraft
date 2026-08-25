package com.sam.talkdraft.onboarding.models

internal sealed interface OnboardingEvents {
    data object OnSkipOnboarding : OnboardingEvents
    data object OnOnboardingCompleted : OnboardingEvents
    data class OnAddToCaptureItems(val item: CaptureIdeaOption) : OnboardingEvents
    data object RequestOpenAppSettings : OnboardingEvents
}
