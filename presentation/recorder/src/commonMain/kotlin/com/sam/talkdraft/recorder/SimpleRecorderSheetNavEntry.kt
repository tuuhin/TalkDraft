package com.sam.talkdraft.recorder

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.sam.talkdraft.designsystem.components.UIEventsSideEffect
import com.sam.talkdraft.navigation.NavDestinationBuilder
import com.sam.talkdraft.navigation.NavDestinations
import com.sam.talkdraft.navigation.scenes.BottomSheetSceneStrategy
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.Singleton

@Singleton(binds = [NavDestinationBuilder::class])
internal class SimpleRecorderSheetNavEntry : NavDestinationBuilder {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun EntryProviderScope<NavKey>.navEntry(backStack: NavBackStack<NavKey>) =
        entry<NavDestinations.CaptureFirstRecording>(
            metadata = BottomSheetSceneStrategy.bottomSheet(isSkipPartiallyExpanded = true),
        ) {

            val viewmodel = koinViewModel<SimpleRecorderViewmodel>()
            val state by viewmodel.screenState.collectAsStateWithLifecycle()
            val waveForm by viewmodel.recorderWaveform.collectAsStateWithLifecycle()
            val duration by viewmodel.recorderDuration.collectAsStateWithLifecycle()

            UIEventsSideEffect(eventsFlow = viewmodel::uiEvents)

            SimpleRecorderSheet(
                screenState = state,
                audioWaveForm = { waveForm },
                recordingDuration = { duration },
                onAction = viewmodel::onEvent,
                onDismiss = { if (backStack.isNotEmpty()) backStack.removeLastOrNull() },
            )
        }
}
