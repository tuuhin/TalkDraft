package com.sam.talkdraft.connectivity

import com.sam.talkdraft.connectivity.models.ConnectivityState
import kotlinx.coroutines.flow.Flow

interface IConnectivityManager {
    val connectivityFlow: Flow<ConnectivityState>
    suspend fun getCurrentState(): ConnectivityState
}
