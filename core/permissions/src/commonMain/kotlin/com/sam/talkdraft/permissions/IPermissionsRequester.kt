package com.sam.talkdraft.permissions

import com.sam.talkdraft.permissions.model.PermissionState
import com.sam.talkdraft.permissions.model.Permissions

interface IPermissionsRequester {

    suspend fun checkPermissionStatus(permission: Permissions): PermissionState
    suspend fun requestPermission(permission: Permissions): PermissionState
    suspend fun requestPermissions(permissions: List<Permissions>): Map<Permissions, PermissionState>
}
