package com.sam.talkdraft.navigation.home_nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal expect fun HomeNestedNavDisplay(
    homeScreen: @Composable () -> Unit,
    recordingScreen: @Composable () -> Unit,
    settingsScreen: @Composable () -> Unit,
    modifier: Modifier = Modifier,
)
