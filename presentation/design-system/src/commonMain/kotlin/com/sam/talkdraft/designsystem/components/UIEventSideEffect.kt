package com.sam.talkdraft.designsystem.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import co.touchlab.kermit.Logger
import com.sam.talkdraft.designsystem.utils.LocalSnackBarState
import com.sam.talkdraft.designsystem.utils.UIEvents
import com.sam.talkdraft.designsystem.utils.toast.PlatformToastProvider
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import org.koin.compose.koinInject

@Composable
fun UIEventsSideEffect(
    eventsFlow: () -> Flow<UIEvents>,
    snackBarState: SnackbarHostState = LocalSnackBarState.current,
    onNavigateBack: () -> Unit = {},
) {

    val lifecycleOwner = LocalLifecycleOwner.current
    val toastProvider = koinInject<PlatformToastProvider>()

    val updatedOnNavigateBack by rememberUpdatedState(newValue = onNavigateBack)

    LaunchedEffect(key1 = lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            eventsFlow()
                .buffer(2, BufferOverflow.DROP_OLDEST)
                .catch { err -> Logger.e(tag = "FLOW_ERROR_IN_SIDE_EFFECT") { err.message ?: "" } }
                .collect { event ->
                    when (event) {
                        is UIEvents.ShowSnackBarWithActions -> {
                            val result = snackBarState.showSnackbar(
                                message = event.message,
                                actionLabel = event.actionText,
                                withDismissAction = event.actionText != null,
                                duration = if (event.long) SnackbarDuration.Long else SnackbarDuration.Short,
                            )
                            when (result) {
                                SnackbarResult.ActionPerformed -> event.action()
                                else -> {}
                            }
                        }

                        is UIEvents.ShowToast -> toastProvider.showToastMessage(event.message)

                        is UIEvents.ShowSnackBar -> snackBarState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Short,
                        )

                        UIEvents.PopScreen -> updatedOnNavigateBack()
                    }
                }
        }
    }
}
