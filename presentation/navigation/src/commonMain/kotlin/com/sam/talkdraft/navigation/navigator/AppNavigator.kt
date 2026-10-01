package com.sam.talkdraft.navigation.navigator

import com.sam.talkdraft.navigation.NavDestinations
import kotlinx.coroutines.flow.Flow

interface AppNavigator {
    suspend fun navigateTo(destination: NavDestinations)
    suspend fun updateBackStack(stack: List<NavDestinations>)
    fun pop(): Boolean
    suspend fun popTo(destination: NavDestinations, inclusive: Boolean = false)
    suspend fun clearAndNavigate(destination: NavDestinations)
}

internal interface AppNavigationObserver {
    val navigationCommands: Flow<NavCommands>
}
