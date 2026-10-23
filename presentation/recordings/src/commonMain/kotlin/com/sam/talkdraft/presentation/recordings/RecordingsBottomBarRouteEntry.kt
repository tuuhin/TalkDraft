package com.sam.talkdraft.presentation.recordings

import androidx.compose.runtime.Composable
import com.sam.talkdraft.designsystem.components.UIEventsSideEffect
import com.sam.talkdraft.navigation.home_nav.PrimaryBottomBarActionBuilder
import com.sam.talkdraft.navigation.home_nav.PrimaryBottomBarNavDestination
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.Singleton

@Singleton(binds = [PrimaryBottomBarActionBuilder::class])
internal class RecordingsBottomBarRouteEntry : PrimaryBottomBarActionBuilder {

    override val contentKey: PrimaryBottomBarNavDestination
        get() = PrimaryBottomBarNavDestination.Recordings

    @Composable
    override fun Content() {
        val viewModel = koinViewModel<RecordingsListViewmodel>()

        UIEventsSideEffect(viewModel::uiEvents)
        RecordingsListScreen()
    }
}
