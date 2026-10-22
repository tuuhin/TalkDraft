package com.sam.talkdraft.navigation.home_nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.sam.talkdraft.navigation.R

internal val PrimaryBottomBarNavDestination.selectedIcon: Painter
    @Composable
    get() = when (this) {
        PrimaryBottomBarNavDestination.Home -> painterResource(R.drawable.ic_home_filled)
        PrimaryBottomBarNavDestination.Recordings -> painterResource(R.drawable.ic_list_filled)
        PrimaryBottomBarNavDestination.Settings -> painterResource(R.drawable.ic_settings_outlined)
    }

internal val PrimaryBottomBarNavDestination.unselectedIcon: Painter
    @Composable
    get() = when (this) {
        PrimaryBottomBarNavDestination.Home -> painterResource(R.drawable.ic_home_outlined)
        PrimaryBottomBarNavDestination.Recordings -> painterResource(R.drawable.ic_list_outlined)
        PrimaryBottomBarNavDestination.Settings -> painterResource(R.drawable.ic_settings_outlined)
    }

internal val PrimaryBottomBarNavDestination.labelText: String
    get() = when (this) {
        PrimaryBottomBarNavDestination.Home -> "Home"
        PrimaryBottomBarNavDestination.Recordings -> "Recordings"
        PrimaryBottomBarNavDestination.Settings -> "Settings"
    }
