package com.sam.talkdraft.feature_onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

@Factory(binds = [IOnboardingPreferences::class])
internal class OnboardingPreferencesImpl(
    @Named("app_settings")
    private val preferences: DataStore<Preferences>,
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
