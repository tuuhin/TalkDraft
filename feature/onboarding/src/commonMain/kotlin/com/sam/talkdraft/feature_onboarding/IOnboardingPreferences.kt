package com.sam.talkdraft.feature_onboarding

import kotlinx.coroutines.flow.Flow

interface IOnboardingPreferences {

    /**
     * A [Flow] emitting a boolean value indicating whether the onboarding flow
     * should be displayed to the user.
     *
     * Emits `true` if onboarding is required, or `false` if it has been completed.
     */
    val showOnboarding: Flow<Boolean>

    /**
     * Marks the onboarding flow as completed
     * Updating this state should cause [showOnboarding] to emit `false`.
     */
    suspend fun markOnboardingCompleted()

    /**
     * Resets the onboarding state back to its initial/uncompleted state.
     * Updating this state should cause [showOnboarding] to emit `true`.
     */
    suspend fun resetOnboardingState()
}
