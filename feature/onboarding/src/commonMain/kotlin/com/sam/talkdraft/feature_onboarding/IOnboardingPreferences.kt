package com.sam.talkdraft.feature_onboarding

import kotlinx.coroutines.flow.Flow

interface IOnboardingPreferences {
    val showOnboarding: Flow<Boolean>
    suspend fun setShowOnboarding(value: Boolean)
}
