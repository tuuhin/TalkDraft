package com.sam.talkdraft.recorder

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.sam.talkdraft.designsystem.components.UIEventsSideEffect
import com.sam.talkdraft.navigation.NavDestinationBuilder
import com.sam.talkdraft.navigation.NavDestinations
import com.sam.talkdraft.navigation.navigator.AppNavigator
import com.sam.talkdraft.navigation.scenes.BottomSheetSceneStrategy
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.Singleton

@Singleton(binds = [NavDestinationBuilder::class])
internal class SimpleRecorderSheetNavEntry : NavDestinationBuilder {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun EntryProviderScope<NavKey>.navEntry() =
        entry<NavDestinations.CaptureFirstRecording>(
            metadata = BottomSheetSceneStrategy.bottomSheet(isSkipPartiallyExpanded = true),
        ) {

            val viewmodel = koinViewModel<SimpleRecorderViewmodel>()
            val navigator = koinInject<AppNavigator>()

            val state by viewmodel.uiState.collectAsStateWithLifecycle()
            val waveForm by viewmodel.recorderWaveform.collectAsStateWithLifecycle()
            val duration by viewmodel.recorderDuration.collectAsStateWithLifecycle()

            UIEventsSideEffect(eventsFlow = viewmodel::uiEvents)

            val backState = rememberNavigationEventState(NavigationEventInfo.None)

            NavigationBackHandler(
                state = backState,
                isBackEnabled = !state.isSavingRecording,
                onBackCancelled = {},
                onBackCompleted = { navigator.pop() },
            )

            SimpleRecorderSheet(
                screenState = state,
                audioWaveForm = { waveForm },
                recordingDuration = { duration },
                onAction = viewmodel::onEvent,
                onDismiss = { navigator.pop() },
            )
        }
}
