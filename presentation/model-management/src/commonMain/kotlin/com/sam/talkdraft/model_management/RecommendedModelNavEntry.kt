package com.sam.talkdraft.model_management

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.sam.talkdraft.designsystem.components.UIEventsSideEffect
import com.sam.talkdraft.navigation.NavDestinationBuilder
import com.sam.talkdraft.navigation.NavDestinations
import com.sam.talkdraft.navigation.navigator.AppNavigator
import com.sam.talkdraft.navigation.scenes.BottomSheetSceneStrategy
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.Singleton
import org.koin.core.parameter.parametersOf

@Singleton(binds = [NavDestinationBuilder::class])
internal class RecommendedModelNavEntry : NavDestinationBuilder {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun EntryProviderScope<NavKey>.navEntry() =
        entry<NavDestinations.RecommendedDownloadModelScreen>(
            metadata = BottomSheetSceneStrategy.bottomSheet(isSkipPartiallyExpanded = true),
        ) { entry ->

            val navigator = koinInject<AppNavigator>()
            val viewmodel = koinViewModel<RecommendedModelViewmodel>(parameters = { parametersOf(entry.modelId) })
            val screenState by viewmodel.screenState.collectAsStateWithLifecycle()

            UIEventsSideEffect(eventsFlow = viewmodel::uiEvents)

            RecommendedModelDownloadSheet(
                onDismiss = { navigator.pop() },
                state = screenState,
                onAction = viewmodel::onEvent,
            )
        }
}
