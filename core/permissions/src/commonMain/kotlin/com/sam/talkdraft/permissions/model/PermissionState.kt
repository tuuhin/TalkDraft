package com.sam.talkdraft.permissions.model

sealed interface PermissionState {
    data class AndroidPermissionState(
        val isGranted: Boolean = false,
        val isShowRational: Boolean = true,
        val isRequestedAtLeastOnce: Boolean = false,
    ) : PermissionState

    data class IosPermissionState(val status: IosPermissionStatus) : PermissionState
}
