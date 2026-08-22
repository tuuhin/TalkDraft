package com.sam.talkdraft.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.connectivity.models.ConnectivityState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory(binds = [IConnectivityManager::class])
internal actual class PlatformConnectivityManager(
    private val context: Context,
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IConnectivityManager {

    private val _connectivityManager by lazy { context.getSystemService<ConnectivityManager>() }

    actual override suspend fun getCurrentState(): ConnectivityState {
        return withContext(dispatchers.io) {
            readNetworkState()
        }
    }

    actual override val connectivityFlow: Flow<ConnectivityState>
        get() = callbackFlow {
            launch {
                val state = readNetworkState()
                send(state)
            }

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    trySend(readNetworkState())
                }

                override fun onLost(network: Network) {
                    trySend(readNetworkState())
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    trySend(readNetworkState())
                }
            }

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            _connectivityManager?.registerNetworkCallback(request, callback)

            awaitClose {
                _connectivityManager?.unregisterNetworkCallback(callback)
            }
        }.flowOn(dispatchers.io)
            .distinctUntilChanged()

    private fun readNetworkState(): ConnectivityState {
        val activeNetwork = _connectivityManager?.activeNetwork ?: return ConnectivityState.OFFLINE
        val capabilities =
            _connectivityManager?.getNetworkCapabilities(activeNetwork) ?: return ConnectivityState.OFFLINE

        // no internet means offline
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            return ConnectivityState.OFFLINE

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectivityState.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectivityState.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectivityState.ETHERNET
            else -> ConnectivityState.OFFLINE
        }
    }
}
