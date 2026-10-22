package com.sam.talkdraft.navigation.home_nav

import androidx.compose.runtime.Composable

interface PrimaryBottomBarActionBuilder {

    val contentKey: PrimaryBottomBarNavDestination

    @Composable
    fun Content()
}
