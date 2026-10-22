package com.sam.talkdraft.navigation.home_nav

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
                        Icon(
                            painter = if (item == selectedRoute) item.selectedIcon
                            else item.unselectedIcon,
                            contentDescription = item.labelText,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    label = { Text(item.labelText) },
                    selected = selectedRoute == item,
                    onClick = { onSelectRoute(item) },
                )
            }
        }
    }
}
