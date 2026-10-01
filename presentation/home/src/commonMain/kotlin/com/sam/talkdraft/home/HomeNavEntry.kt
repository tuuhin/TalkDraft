package com.sam.talkdraft.home

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.sam.talkdraft.navigation.NavDestinationBuilder
import com.sam.talkdraft.navigation.NavDestinations
import org.koin.core.annotation.Singleton

@Singleton(binds = [NavDestinationBuilder::class])
internal class HomeNavEntry : NavDestinationBuilder {

    override fun EntryProviderScope<NavKey>.navEntry() =
        entry<NavDestinations.HomeScreen> {
            HomeScreen()
        }
}
