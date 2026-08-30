package com.sam.talkdraft.feature_onboarding

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.sam.talkdraft.datastore.model.AppSettingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory

@Factory(binds = [IOnboardingPreferences::class])
internal class OnboardingPreferencesImpl(
    private val preferences: AppSettingsDataStore,
) : IOnboardingPreferences {

    private val boolKey = booleanPreferencesKey(ONBOARDING_KEY)

    override suspend fun setShowOnboarding(value: Boolean) {
        preferences.edit { prefs ->
            prefs[boolKey] = value
        }
    }

    override val showOnboarding: Flow<Boolean>
        get() = preferences.data.map { prefs -> prefs[boolKey] ?: true }

    companion object {
        private const val ONBOARDING_KEY = "SHOW_ONBOARDING"
    }
}
