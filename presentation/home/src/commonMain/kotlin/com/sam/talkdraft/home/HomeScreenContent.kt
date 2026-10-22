package com.sam.talkdraft.home

import androidx.compose.runtime.Composable
import com.sam.talkdraft.designsystem.components.UIEventsSideEffect
import com.sam.talkdraft.navigation.home_nav.PrimaryBottomBarActionBuilder
import com.sam.talkdraft.navigation.home_nav.PrimaryBottomBarNavDestination
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.Singleton

@Singleton(binds = [PrimaryBottomBarActionBuilder::class])
internal class HomeScreenContent : PrimaryBottomBarActionBuilder {

    override val contentKey: PrimaryBottomBarNavDestination
        get() = PrimaryBottomBarNavDestination.Home

    @Composable
    override fun Content() {
        val viewModel = koinViewModel<HomeScreenViewmodel>()

        UIEventsSideEffect(viewModel::uiEvents)
        HomeScreen()
    }
}
