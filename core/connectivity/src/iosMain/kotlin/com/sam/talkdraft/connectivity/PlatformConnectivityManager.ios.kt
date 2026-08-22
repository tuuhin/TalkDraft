package com.sam.talkdraft.connectivity

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.connectivity.models.ConnectivityState
import kotlin.coroutines.resume
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import platform.Network.nw_interface_type_cellular
import platform.Network.nw_interface_type_wifi
import platform.Network.nw_interface_type_wired
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.Network.nw_path_uses_interface_type
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import platform.darwin.dispatch_get_global_queue

private const val TAG = "CONNECTIVITY_MANAGER"

@Factory(binds = [IConnectivityManager::class])
internal actual class PlatformConnectivityManager(
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IConnectivityManager {

    actual override suspend fun getCurrentState(): ConnectivityState {
        return withContext(dispatchers.io) {
            suspendCancellableCoroutine { cont ->
                val monitor = nw_path_monitor_create()
                val queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u)
                nw_path_monitor_set_update_handler(monitor) { path ->
                    val isConnected = nw_path_get_status(path) == nw_path_status_satisfied

                    val state = if (!isConnected) {
                        ConnectivityState.OFFLINE
                    } else when {
                        nw_path_uses_interface_type(path, nw_interface_type_wifi) -> ConnectivityState.WIFI
                        nw_path_uses_interface_type(path, nw_interface_type_cellular) -> ConnectivityState.CELLULAR
                        nw_path_uses_interface_type(path, nw_interface_type_wired) -> ConnectivityState.ETHERNET
                        else -> ConnectivityState.OFFLINE
                    }


                    nw_path_monitor_cancel(monitor)

                    if (cont.isActive) {
                        Logger.d(tag = TAG) { "NETWORK STATE READ" }
                        cont.resume(state)
                    }

                    nw_path_monitor_set_queue(monitor, queue)
                    nw_path_monitor_start(monitor)

                    cont.invokeOnCancellation {
                        Logger.d(tag = TAG) { "NETWORK STATE READ CANCELLED" }
                        nw_path_monitor_cancel(monitor)
                    }
                }
            }
        }
    }

    actual override val connectivityFlow: Flow<ConnectivityState>
        get() = channelFlow {
            val monitor = nw_path_monitor_create()
            val queue = dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0UL)

            nw_path_monitor_set_update_handler(monitor) { path ->
                val isConnected = nw_path_get_status(path) == nw_path_status_satisfied

                val finalState = if (!isConnected) ConnectivityState.OFFLINE
                else when {
                    nw_path_uses_interface_type(path, nw_interface_type_wifi) -> ConnectivityState.WIFI
                    nw_path_uses_interface_type(path, nw_interface_type_cellular) -> ConnectivityState.CELLULAR
                    nw_path_uses_interface_type(path, nw_interface_type_wired) -> ConnectivityState.ETHERNET
                    else -> ConnectivityState.OFFLINE
                }
                trySend(finalState)
            }

            nw_path_monitor_set_queue(monitor, queue)
            nw_path_monitor_start(monitor)
            Logger.d(tag = TAG) { "PATH MONITOR QUEUED ON GLOBAL QUEUE" }

            awaitClose {
                Logger.d(tag = TAG) { "PATH MONITOR CANELLED" }
                nw_path_monitor_cancel(monitor)
            }
        }.distinctUntilChanged()
}


