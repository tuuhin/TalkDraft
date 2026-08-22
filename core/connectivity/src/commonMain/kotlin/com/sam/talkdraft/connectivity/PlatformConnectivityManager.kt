package com.sam.talkdraft.connectivity

import com.sam.talkdraft.connectivity.models.ConnectivityState
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory(binds = [IConnectivityManager::class])
internal expect class PlatformConnectivityManager : IConnectivityManager {
    override suspend fun getCurrentState(): ConnectivityState
    override val connectivityFlow: Flow<ConnectivityState>
}
