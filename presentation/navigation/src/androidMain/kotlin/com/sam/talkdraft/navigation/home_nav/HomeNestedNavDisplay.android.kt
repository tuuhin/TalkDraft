package com.sam.talkdraft.navigation.home_nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import androidx.window.core.layout.WindowSizeClass
import com.sam.talkdraft.designsystem.utils.LocalAnimatedContentScope
import com.sam.talkdraft.designsystem.utils.LocalSharedTransitionScope
import com.sam.talkdraft.designsystem.utils.LocalSnackBarState
import com.sam.talkdraft.designsystem.utils.sharedTransitionRenderInOverlay
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@OptIn(ExperimentalSerializationApi::class)
@Composable
internal actual fun HomeNestedNavDisplay(
    homeScreen: @Composable (() -> Unit),
    recordingScreen: @Composable (() -> Unit),
    settingsScreen: @Composable (() -> Unit),
    modifier: Modifier,
) {
    val contentScope = LocalAnimatedContentScope.current
    val snackBarHost = LocalSnackBarState.current

    val backStack = rememberNavBackStack(
        configuration = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclassesOfSealed<PrimaryBottomBarNavDestination>()
                }
            }
        },
        PrimaryBottomBarNavDestination.Home,
    )

    val selectedTab by remember(backStack) {
        derivedStateOf {
            backStack.filterIsInstance<PrimaryBottomBarNavDestination>().lastOrNull()
        }
    }

    val navItems = remember {
        persistentListOf(
            PrimaryBottomBarNavDestination.Home,
            PrimaryBottomBarNavDestination.Recordings,
            PrimaryBottomBarNavDestination.Settings,
        )
    }

    val onSelectTab: (PrimaryBottomBarNavDestination) -> Unit = { targetTab ->
        if (selectedTab != targetTab) {
            backStack.clear()
            backStack.add(targetTab)
        }
    }

    val windowInfo = currentWindowAdaptiveInfoV2()

    Scaffold(
        modifier = modifier,
        bottomBar = {
            AppNavigationBar(
                items = navItems,
                onSelectRoute = onSelectTab,
                selectedRoute = selectedTab ?: PrimaryBottomBarNavDestination.Home,
                showNavBar = !windowInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND),
                modifier = Modifier.sharedTransitionRenderInOverlay(1f)
                    .then(
                        if (contentScope == null)
                            Modifier else with(contentScope) {
                            Modifier.animateEnterExit(
                                enter = slideInVertically { height -> height } + fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
                                exit = slideOutVertically { height -> height } + fadeOut(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
                            )
                        },
                    ),
            )
        },
        contentWindowInsets = WindowInsets.navigationBars,
        snackbarHost = { SnackbarHost(snackBarHost) },
    ) { scPadding ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize()
                .padding(scPadding),
        ) {
            AppNavigationRail(
                items = navItems,
                onSelectRoute = onSelectTab,
                selectedRoute = selectedTab ?: PrimaryBottomBarNavDestination.Home,
                showNavRail = windowInfo.windowSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND),
                modifier = Modifier.sharedTransitionRenderInOverlay(1f)
                    .then(
                        if (contentScope == null)
                            Modifier else with(contentScope) {
                            Modifier.animateEnterExit(
                                enter = slideInHorizontally { width -> -width } + fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
                                exit = slideOutHorizontally { width -> -width } + fadeOut(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
                            )
                        },
                    ),
            )
            CompositionLocalProvider(LocalAnimatedContentScope provides contentScope) {
                NavDisplay(
                    backStack = backStack,
                    onBack = { backStack.removeAll { it != PrimaryBottomBarNavDestination.Home } },
                    modifier = modifier,
                    sharedTransitionScope = LocalSharedTransitionScope.current,
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator(),
                    ),
                    transitionSpec = {
                        fadeIn(initialAlpha = .8f) togetherWith fadeOut(targetAlpha = .1f)
                    },
                    predictivePopTransitionSpec = {
                        EnterTransition.None togetherWith ExitTransition.None
                    },
                    contentAlignment = Alignment.Center,
                    entryProvider = entryProvider {
                        entry<PrimaryBottomBarNavDestination.Home> {
                            homeScreen()
                        }
                        entry<PrimaryBottomBarNavDestination.Recordings> {
                            recordingScreen()
                        }
                        entry<PrimaryBottomBarNavDestination.Settings> {
                            settingsScreen()
                        }
                    },
                )
            }
        }
    }
}

@Preview
@PreviewLightDark
@Composable
private fun HomeNestedNavDisplayPreview() {
    HomeNestedNavDisplay(homeScreen = {}, recordingScreen = {}, settingsScreen = {})
}
