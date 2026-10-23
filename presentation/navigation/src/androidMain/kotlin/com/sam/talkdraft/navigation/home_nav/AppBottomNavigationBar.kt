package com.sam.talkdraft.navigation.home_nav

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun AppNavigationBar(
    items: ImmutableList<PrimaryBottomBarNavDestination>,
    onSelectRoute: (PrimaryBottomBarNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    showNavBar: Boolean = false,
    selectedRoute: PrimaryBottomBarNavDestination = PrimaryBottomBarNavDestination.Home,
    containerColor: Color = NavigationBarDefaults.containerColor,
    elevation: Dp = NavigationBarDefaults.Elevation,
) {
    val spatialSpec = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    val effectsSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()

    AnimatedVisibility(
        visible = showNavBar,
        enter = slideInVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()) { height -> height },
        exit = slideOutVertically(animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()) { height -> height },
        modifier = modifier,
    ) {
        NavigationBar(
            containerColor = containerColor,
            tonalElevation = elevation,
            contentColor = contentColorFor(containerColor),
        ) {
            items.forEach { item ->
                NavigationBarItem(
                    icon = {
                        AnimatedContent(
                            targetState = item == selectedRoute,
                            transitionSpec = {
                                fadeIn(animationSpec = effectsSpec) +
                                    scaleIn(initialScale = 0.8f, animationSpec = spatialSpec) togetherWith
                                    fadeOut(animationSpec = effectsSpec) +
                                    scaleOut(targetScale = 0.8f, animationSpec = spatialSpec)
                            },
                            label = "NavIconExpressiveAnimation",
                        ) { isSelected ->
                            Icon(
                                painter = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.labelText,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    },
                    label = { Text(item.labelText) },
                    selected = selectedRoute == item,
                    onClick = { onSelectRoute(item) },
                )
            }
        }
    }
}
