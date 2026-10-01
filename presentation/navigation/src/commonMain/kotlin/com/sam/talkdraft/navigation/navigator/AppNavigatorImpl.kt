package com.sam.talkdraft.navigation.navigator

import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.navigation.NavDestinations
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single

@Single(binds = [AppNavigator::class, AppNavigationObserver::class])
internal class AppNavigatorImpl(
    private val dispatchers: IPlatformCoroutineDispatchers,
) : AppNavigator, AppNavigationObserver {

    private val _channel = Channel<NavCommands>(
        Channel.BUFFERED,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val navigationCommands: Flow<NavCommands> = _channel.receiveAsFlow()

    override suspend fun navigateTo(destination: NavDestinations) = withContext(dispatchers.mainImmediate) {
        _channel.send(NavCommands.NavigateTo(destination))
    }

    override suspend fun updateBackStack(stack: List<NavDestinations>) = withContext(dispatchers.mainImmediate) {
        _channel.send(NavCommands.UpdateBackStack(stack))
    }


    override fun pop(): Boolean {
        _channel.trySend(NavCommands.Pop)
        return true
    }

    override suspend fun popTo(destination: NavDestinations, inclusive: Boolean) =
        withContext(dispatchers.mainImmediate) {
            _channel.send(NavCommands.PopTo(destination, inclusive))
        }

    override suspend fun clearAndNavigate(destination: NavDestinations) = withContext(dispatchers.mainImmediate) {
        _channel.send(NavCommands.ClearAndNavigate(destination))
    }
}
