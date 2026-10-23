package com.sam.talkdraft.navigation.home_nav

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sam.talkdraft.navigation.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch

@Composable
internal fun AppNavigationRail(
    items: ImmutableList<PrimaryBottomBarNavDestination>,
    onSelectRoute: (PrimaryBottomBarNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    showNavRail: Boolean = false,
    selectedRoute: PrimaryBottomBarNavDestination = PrimaryBottomBarNavDestination.Home,
    initialRailExpanded: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    val scope = rememberCoroutineScope()

    val state = rememberWideNavigationRailState(
        initialValue = if (initialRailExpanded) WideNavigationRailValue.Expanded
        else WideNavigationRailValue.Collapsed,
    )

    val spatialSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val effectsSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()

    AnimatedVisibility(
        visible = showNavRail,
        enter = slideInHorizontally(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()) { width -> -width },
        exit = slideOutHorizontally(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()) { width -> -width },
        modifier = modifier,
    ) {
        WideNavigationRail(
            state = state,
            colors = WideNavigationRailDefaults.colors(
                containerColor = containerColor,
                contentColor = contentColorFor(containerColor),
            ),
            arrangement = if (state.currentValue == WideNavigationRailValue.Expanded) Arrangement.spacedBy(4.dp)
            else WideNavigationRailDefaults.arrangement,
            header = {
                IconButton(onClick = { scope.launch { state.toggle() } }) {
                    AnimatedContent(
                        targetState = (state.targetValue == WideNavigationRailValue.Expanded),
                        transitionSpec = {
                            fadeIn(animationSpec = effectsSpec) +
                                scaleIn(initialScale = 0.8f, animationSpec = spatialSpec) togetherWith
                                fadeOut(animationSpec = effectsSpec) + scaleOut(
                                targetScale = 0.8f,
                                animationSpec = spatialSpec,
                            )
                        },
                        label = "RailToggleAnimation",
                    ) { isExpanded ->
                        Icon(
                            painter = painterResource(
                                if (isExpanded) R.drawable.ic_menu_close
                                else R.drawable.ic_menu_open,
                            ),
                            contentDescription = if (isExpanded) "Menu close" else "Menu open",
                        )
                    }
                }
            },
        ) {
            items.forEach { item ->
                WideNavigationRailItem(
                    railExpanded = state.currentValue == WideNavigationRailValue.Expanded,
                    iconPosition = if (state.currentValue == WideNavigationRailValue.Expanded)
                        NavigationItemIconPosition.Start else NavigationItemIconPosition.Top,
                    label = { Text(item.labelText) },
                    icon = {
                        Icon(
                            painter = if (item == selectedRoute) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.labelText,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    selected = selectedRoute == item,
                    onClick = { onSelectRoute(item) },
                )
            }
        }
    }
}
