package com.sam.talkdraft.navigation

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.sam.talkdraft.designsystem.utils.LocalSharedTransitionScope
import com.sam.talkdraft.navigation.navigator.AppNavigationObserver
import com.sam.talkdraft.navigation.navigator.NavCommands
import com.sam.talkdraft.navigation.scenes.BottomSheetSceneStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.compose.getKoin
import org.koin.compose.koinInject

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun AppRootNavHost(
    modifier: Modifier = Modifier,
    startDestinations: NavDestinations = NavDestinations.OnBoardingScreen,
) {
    val koin = getKoin()
    val navigator = koinInject<AppNavigationObserver>()
    val destinations = remember(koin) {
        koin.getAll<NavDestinationBuilder>()
    }

    val backStack = rememberNavBackStack(
        configuration = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclassesOfSealed<NavDestinations>()
                }
            }
        },
        startDestinations,
    )

    HandleNavigatorCommands(navigator, backStack)

    val spatialEffect = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val fastSpatialFloatEffect = MaterialTheme.motionScheme.fastSpatialSpec<Float>()

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        sharedTransitionScope = LocalSharedTransitionScope.current,
        sceneStrategies = listOf(DialogSceneStrategy(), BottomSheetSceneStrategy(), SinglePaneSceneStrategy()),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        contentAlignment = Alignment.Center,
        transitionSpec = {
            scaleIn(
                animationSpec = tween(durationMillis = 120, easing = FastOutLinearInEasing),
                initialScale = 0.9f,
            ) + fadeIn(animationSpec = spatialEffect) togetherWith
                scaleOut(
                    animationSpec = tween(durationMillis = 120, easing = EaseOutCubic),
                    targetScale = 1.1f,
                ) + fadeOut(fastSpatialFloatEffect)
        },
        popTransitionSpec = {
            scaleIn(
                animationSpec = tween(durationMillis = 120, easing = FastOutLinearInEasing),
                initialScale = 1.1f,
            ) + fadeIn(animationSpec = spatialEffect) togetherWith
                scaleOut(
                    animationSpec = tween(durationMillis = 120, easing = EaseOutCubic),
                    targetScale = 0.9f,
                ) + fadeOut(fastSpatialFloatEffect)
        },
        predictivePopTransitionSpec = {
            scaleIn(
                animationSpec = tween(durationMillis = 120, easing = FastOutLinearInEasing),
                initialScale = 1.1f,
            ) + fadeIn(animationSpec = spatialEffect) togetherWith
                scaleOut(
                    animationSpec = tween(durationMillis = 120, easing = EaseOutCubic),
                    targetScale = 0.9f,
                ) + fadeOut(fastSpatialFloatEffect)
        },
        entryProvider = entryProvider {
            destinations.forEach { builder ->
                builder.apply { navEntry() }
            }
        },
    )
}


@Composable
private fun HandleNavigatorCommands(
    navigator: AppNavigationObserver,
    backStack: NavBackStack<NavKey>,
) {
    LaunchedEffect(navigator, backStack) {
        navigator.navigationCommands.collect { command ->
            when (command) {
                is NavCommands.NavigateTo -> backStack.add(command.destination)
                is NavCommands.Pop if (backStack.size > 1) -> backStack.removeLast()
                is NavCommands.PopTo -> {
                    val index = backStack.indexOfLast { it == command.destination }
                    if (index != -1) {
                        val targetIndex = if (command.inclusive) index else index + 1
                        while (backStack.size > targetIndex) {
                            backStack.removeLast()
                        }
                    }
                }

                is NavCommands.ClearAndNavigate -> {
                    backStack.clear()
                    backStack.add(command.destination)
                }

                is NavCommands.UpdateBackStack -> {
                    backStack.clear()
                    backStack.addAll(command.stack)
                }

                else -> {}
            }
        }
    }
}
