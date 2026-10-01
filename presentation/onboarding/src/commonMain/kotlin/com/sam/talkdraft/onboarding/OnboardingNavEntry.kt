package com.sam.talkdraft.onboarding

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.sam.talkdraft.designsystem.components.UIEventsSideEffect
import com.sam.talkdraft.navigation.NavDestinationBuilder
import com.sam.talkdraft.navigation.NavDestinations
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.Singleton

@Singleton(binds = [NavDestinationBuilder::class])
internal class OnboardingNavEntry : NavDestinationBuilder {

    override fun EntryProviderScope<NavKey>.navEntry() = entry<NavDestinations.OnBoardingScreen> {

        val viewmodel = koinViewModel<OnBoardingViewmodel>()
        val screenState by viewmodel.screenSate.collectAsStateWithLifecycle()
        val initialScene by viewmodel.initialScene.collectAsStateWithLifecycle()

        UIEventsSideEffect(eventsFlow = viewmodel::uiEvents)

        OnBoardingScreen(
            state = screenState,
            initialScene = initialScene,
            onEvent = viewmodel::onEvent,
        )
    }
}
