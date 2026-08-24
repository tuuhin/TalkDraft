package com.sam.talkdraft.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sam.talkdraft.designsystem.utils.UIEvents
import com.sam.talkdraft.feature_onboarding.IOnboardingPreferences
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel

@KoinViewModel
internal class OnBoardingViewmodel(
    private val onboarding: IOnboardingPreferences,
) : ViewModel() {

    val onboardinStatus = onboarding.showOnboarding.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = false,
    )

    val uiEvents: SharedFlow<UIEvents>
        field = MutableSharedFlow<UIEvents>()
}
