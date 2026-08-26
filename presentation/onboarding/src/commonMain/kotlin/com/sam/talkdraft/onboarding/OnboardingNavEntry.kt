package com.sam.talkdraft.onboarding

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sam.talkdraft.common.platform.IPlatformTargetProvider
import com.sam.talkdraft.designsystem.components.UIEventsSideEffect
import com.sam.talkdraft.navigation.NavDestinationBuilder
import com.sam.talkdraft.navigation.NavDestinations
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.Singleton

@Singleton(binds = [NavDestinationBuilder::class])
internal class OnboardingNavEntry : NavDestinationBuilder {

    override fun EntryProviderScope<NavKey>.navEntry(
        backStack: NavBackStack<NavKey>,
    ) = entry<NavDestinations.OnBoardingScreen> {

        val viewmodel = koinViewModel<OnBoardingViewmodel>()
        val captureIdeas by viewmodel.captureIdeas.collectAsStateWithLifecycle()

        val platformProvider = koinInject<IPlatformTargetProvider>()

        UIEventsSideEffect(eventsFlow = viewmodel::uiEvents)

        OnBoardingScreen(
            capturedIdeas = captureIdeas,
            platform = platformProvider.target(),
            onEvent = viewmodel::onEvent,
        )
    }
}
