package com.sam.talkdraft.presentation.settings

import androidx.compose.runtime.Composable
import com.sam.talkdraft.designsystem.components.UIEventsSideEffect
import com.sam.talkdraft.navigation.home_nav.PrimaryBottomBarActionBuilder
import com.sam.talkdraft.navigation.home_nav.PrimaryBottomBarNavDestination
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.Singleton

@Singleton(binds = [PrimaryBottomBarActionBuilder::class])
internal class SettingsBottomBarRouteEntry : PrimaryBottomBarActionBuilder {

    override val contentKey: PrimaryBottomBarNavDestination
        get() = PrimaryBottomBarNavDestination.Settings

    @Composable
    override fun Content() {
        val viewModel = koinViewModel<SettingsViewmodel>()

        UIEventsSideEffect(viewModel::uiEvents)

        SettingsScreen(onEvent = viewModel::onEvent)
    }
}
