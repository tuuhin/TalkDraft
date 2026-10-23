package com.sam.talkdraft.navigation.home_nav

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.sam.talkdraft.designsystem.utils.LocalAnimatedContentScope
import com.sam.talkdraft.navigation.NavDestinationBuilder
import com.sam.talkdraft.navigation.NavDestinations
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.getKoin
import org.koin.core.annotation.Singleton
import talkdraft.presentation.navigation.generated.resources.Res
import talkdraft.presentation.navigation.generated.resources.ic_empty_box

@Singleton(binds = [NavDestinationBuilder::class])
internal class HomeNestedNavEntry : NavDestinationBuilder {

    override fun EntryProviderScope<NavKey>.navEntry() = entry<NavDestinations.HomeScreen> {

        val koin = getKoin()
        val scope = LocalNavAnimatedContentScope.current

        val (home, recording, settings) = remember {
            val builder = koin.getAll<PrimaryBottomBarActionBuilder>()
                .sortedBy { it.contentKey.order }

            val home = builder.getOrNull(0)
            val recordings = builder.getOrNull(1)
            val settings = builder.getOrNull(2)

            Triple(home, recordings, settings)
        }

        CompositionLocalProvider(LocalAnimatedContentScope provides scope) {
            HomeNestedNavDisplay(
                homeScreen = { home?.Content() ?: EmptyContent() },
                recordingScreen = { recording?.Content() ?: EmptyContent() },
                settingsScreen = { settings?.Content() ?: EmptyContent() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }


    @Composable
    private fun EmptyContent(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column {
                Image(
                    painter = painterResource(Res.drawable.ic_empty_box),
                    contentDescription = "Empty content box",
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.size(96.dp),
                )
                Spacer(modifier = Modifier.height(30.dp))
                Text(
                    text = "No content was set",
                    style = MaterialTheme.typography.bodyMediumEmphasized,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Missing content for the given tab",
                    style = MaterialTheme.typography.labelLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
