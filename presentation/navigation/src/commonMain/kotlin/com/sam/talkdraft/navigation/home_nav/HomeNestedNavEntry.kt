package com.sam.talkdraft.navigation.home_nav

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.sam.talkdraft.navigation.NavDestinationBuilder
import com.sam.talkdraft.navigation.NavDestinations
import org.koin.compose.getKoin
import org.koin.core.annotation.Singleton

@Singleton(binds = [NavDestinationBuilder::class])
internal class HomeNestedNavEntry : NavDestinationBuilder {

    override fun EntryProviderScope<NavKey>.navEntry() = entry<NavDestinations.HomeScreen> {

        val koin = getKoin()

        val (home, recording, settings) = remember {
            val builder = koin.getAll<PrimaryBottomBarActionBuilder>()
                .sortedBy { it.contentKey.order }

            val home = builder.getOrNull(0)
            val recordings = builder.getOrNull(1)
            val settings = builder.getOrNull(2)

            Triple(home, recordings, settings)
        }

        HomeNestedNavDisplay(
            homeScreen = { home?.Content() },
            recordingScreen = { recording?.Content() },
            settingsScreen = { settings?.Content() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
