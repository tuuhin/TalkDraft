package com.sam.talkdraft.navigation.navigator

import com.sam.talkdraft.navigation.NavDestinations

sealed interface NavCommands {
    data class NavigateTo(val destination: NavDestinations) : NavCommands
    data class UpdateBackStack(val stack: List<NavDestinations>) : NavCommands
    data object Pop : NavCommands
    data class PopTo(val destination: NavDestinations, val inclusive: Boolean = false) : NavCommands
    data class ClearAndNavigate(val destination: NavDestinations) : NavCommands
}
