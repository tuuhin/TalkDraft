package com.sam.talkdraft.feature_onboarding

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import co.touchlab.kermit.Logger
import com.sam.talkdraft.datastore.model.AppSettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Factory

@Factory(binds = [IOnboardingPreferences::class])
internal class OnboardingPreferencesImpl(
    private val preferences: AppSettingsDataStore,
) : IOnboardingPreferences {

    private val _onboardingKey = booleanPreferencesKey(ONBOARDING_KEY)

    override suspend fun markOnboardingCompleted() {
        Logger.d(tag = TAG) { "ONBOARDING COMPLETED" }
        preferences.edit { prefs ->
            prefs[_onboardingKey] = false
        }
    }

    override suspend fun resetOnboardingState() {
        Logger.d(tag = TAG) { "ONBOARDING STATE RESET" }
        preferences.edit { prefs -> prefs[_onboardingKey] = true }
    }

    override val showOnboarding: Flow<Boolean>
        get() = preferences.data.map { prefs -> prefs[_onboardingKey] ?: true }
            .onEach { Logger.d(tag = TAG) { "ONBOARDING STATE $it" } }

    companion object {
        private const val TAG = "ONBOARDING"
        private const val ONBOARDING_KEY = "SHOW_ONBOARDING"
    }
}
