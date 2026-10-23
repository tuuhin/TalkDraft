package com.sam.talkdraft.navigation.home_nav

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Immutable
@Serializable
sealed class PrimaryBottomBarNavDestination(internal val order: Int) : NavKey {

    @Serializable
    data object Home : PrimaryBottomBarNavDestination(0)

    @Serializable
    data object Recordings : PrimaryBottomBarNavDestination(1)

    @Serializable
    data object Settings : PrimaryBottomBarNavDestination(2)
}
