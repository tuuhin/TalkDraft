package com.sam.talkdraft.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey

interface NavDestinationBuilder {

    fun EntryProviderScope<NavKey>.navEntry()
}
