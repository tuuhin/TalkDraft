package com.sam.talkdraft.app

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.sam.talkdraft.app.composables.AndroidAppUpdateRequiredDialog
import com.sam.talkdraft.app.viewmodel.AppCommonViewmodel
import com.sam.talkdraft.designsystem.theme.TalkDraftTheme
import com.sam.talkdraft.designsystem.utils.LocalPostureInfo
import com.sam.talkdraft.designsystem.utils.LocalSharedTransitionScope
import com.sam.talkdraft.designsystem.utils.LocalSnackBarState
import com.sam.talkdraft.designsystem.utils.LocalWindowSizeInfo
import com.sam.talkdraft.navigation.AppRootNavHost
import com.sam.talkdraft.navigation.NavDestinations
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    modifier: Modifier = Modifier,
    showOnboarding: Boolean = true,
    showContentForIos: Boolean = true,
) {
    val viewmodel = koinViewModel<AppCommonViewmodel>()
    val windowInfo = currentWindowAdaptiveInfoV2()
    val snackBarHostState = remember { SnackbarHostState() }


    val shouldShowMainContent = when {
        viewmodel.isAndroid -> !viewmodel.showAppUpdateRequiredDialog
        viewmodel.isIos -> showContentForIos
        else -> false
    }

    TalkDraftTheme {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = modifier.fillMaxSize(),
        ) {
            TalkDraftTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = modifier.fillMaxSize(),
                ) {
                    if (viewmodel.isAndroid && viewmodel.showAppUpdateRequiredDialog)
                        AndroidAppUpdateRequiredDialog(onUpdate = {})
                    else if (shouldShowMainContent) SharedTransitionLayout {
                        CompositionLocalProvider(
                            LocalSnackBarState provides snackBarHostState,
                            LocalSharedTransitionScope provides this,
                            LocalWindowSizeInfo provides windowInfo.windowSizeClass,
                            LocalPostureInfo provides windowInfo.windowPosture,
                        ) {
                            val startDestination = if (showOnboarding) {
                                NavDestinations.OnBoardingScreen
                            } else {
                                NavDestinations.HomeScreen
                            }

                            AppRootNavHost(startDestinations = startDestination)
                        }
                    }

                }
            }
        }
    }
}
