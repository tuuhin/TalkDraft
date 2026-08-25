package com.sam.talkdraft.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sam.talkdraft.designsystem.utils.UIEvents
import com.sam.talkdraft.feature_onboarding.IOnboardingPreferences
import com.sam.talkdraft.onboarding.models.CaptureIdeaOption
import com.sam.talkdraft.onboarding.models.OnboardingEvents
import com.sam.talkdraft.onboarding.util.IAppSettingsProvider
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class OnBoardingViewmodel(
    private val onboarding: IOnboardingPreferences,
    private val appSettings: IAppSettingsProvider,
) : ViewModel() {

    val uiEvents: SharedFlow<UIEvents>
        field = MutableSharedFlow<UIEvents>()

    private val _captureIdeas = MutableStateFlow(listOf(CaptureIdeaOption.BRAIN_STORMING))
    val captureIdeas = _captureIdeas.map {
        it.toImmutableSet()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(10_000),
        initialValue = persistentSetOf(),
    )

    fun onEvent(event: OnboardingEvents) = viewModelScope.launch {
        when (event) {
            OnboardingEvents.OnSkipOnboarding -> {}
            OnboardingEvents.OnOnboardingCompleted -> {}
            is OnboardingEvents.OnAddToCaptureItems -> {
                _captureIdeas.update { old ->
                    if (event.item in old) old.filter { it != event.item }
                    else old + event.item
                }
            }

            OnboardingEvents.RequestOpenAppSettings -> appSettings.openSettings()
        }
    }
}
