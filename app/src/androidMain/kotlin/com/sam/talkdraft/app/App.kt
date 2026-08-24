package com.sam.talkdraft.app

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.sam.talkdraft.designsystem.theme.TalkDraftTheme
import com.sam.talkdraft.designsystem.utils.LocalPostureInfo
import com.sam.talkdraft.designsystem.utils.LocalSharedTransitionScope
import com.sam.talkdraft.designsystem.utils.LocalSnackBarState
import com.sam.talkdraft.designsystem.utils.LocalWindowSizeInfo
import com.sam.talkdraft.navigation.AppRootNavHost
import com.sam.talkdraft.navigation.NavDestinations

@Composable
fun App(
    modifier: Modifier = Modifier,
    showOnboarding: Boolean = false,
) {
    val windowInfo = currentWindowAdaptiveInfoV2()
    val snackBarHostState = remember { SnackbarHostState() }

    TalkDraftTheme {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = modifier,
        ) {
            SharedTransitionLayout {
                CompositionLocalProvider(
                    LocalSnackBarState provides snackBarHostState,
                    LocalSharedTransitionScope provides this,
                    LocalWindowSizeInfo provides windowInfo.windowSizeClass,
                    LocalPostureInfo provides windowInfo.windowPosture,
                ) {
                    AppRootNavHost(startDestinations = if (showOnboarding) NavDestinations.OnBoardingScreen else NavDestinations.HomeScreen)
                }
            }
        }
    }
}
