package com.sam.talkdraft.permissions

import com.sam.talkdraft.permissions.model.PermissionState
import com.sam.talkdraft.permissions.model.Permissions
import org.koin.core.annotation.Factory

@Factory(binds = [IPermissionsRequester::class])
internal expect class PermissionsRequesterImpl : IPermissionsRequester {
    override suspend fun checkPermissionStatus(permission: Permissions): PermissionState
    override suspend fun requestPermission(permission: Permissions): PermissionState
    override suspend fun requestPermissions(permissions: List<Permissions>): Map<Permissions, PermissionState>
}
