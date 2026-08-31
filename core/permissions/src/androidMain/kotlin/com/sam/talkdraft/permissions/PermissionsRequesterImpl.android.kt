package com.sam.talkdraft.permissions

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.content.edit
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.permissions.controller.IPermissionController
import com.sam.talkdraft.permissions.model.PermissionState
import com.sam.talkdraft.permissions.model.Permissions
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory

@Factory(binds = [IPermissionsRequester::class])
internal actual class PermissionsRequesterImpl(
    private val context: Context,
    private val controller: IPermissionController,
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IPermissionsRequester {

    private val prefs by lazy { context.getSharedPreferences("permissions_request_prefs", Context.MODE_PRIVATE) }

    actual override suspend fun checkPermissionStatus(permission: Permissions): PermissionState {
        val permission = permission.toPermissionName ?: return PermissionState.AndroidPermissionState(
            isGranted = true,
            isShowRational = true,
            isRequestedAtLeastOnce = true,
        )

        val isGranted = ContextCompat.checkSelfPermission(context, permission) == PermissionChecker.PERMISSION_GRANTED
        val shouldShowRational = controller.shouldShowRational(permission)
        val isRequested = withContext(dispatchers.io) { prefs.getBoolean(permission, false) }
        return PermissionState.AndroidPermissionState(
            isGranted,
            isShowRational = shouldShowRational,
            isRequestedAtLeastOnce = isRequested,
        )
    }

    actual override suspend fun requestPermission(permission: Permissions): PermissionState {
        val name = permission.toPermissionName ?: return PermissionState.AndroidPermissionState(
            isGranted = true,
            isShowRational = false,
            isRequestedAtLeastOnce = true,
        )

        markPermissionAsRequested(name)

        val result = controller.requestPermission(permission = arrayOf(name))
        val status = result.getOrElse(name) { false }
        val shouldShowRational = controller.shouldShowRational(name)

        return PermissionState.AndroidPermissionState(
            isGranted = status,
            isShowRational = shouldShowRational,
            isRequestedAtLeastOnce = true,
        )
    }

    actual override suspend fun requestPermissions(permissions: List<Permissions>): Map<Permissions, PermissionState> {
        val names = permissions.mapNotNull { it.toPermissionName }.toTypedArray()
        names.forEach { markPermissionAsRequested(it) }

        val result = controller.requestPermission(permission = names)
        return result.mapNotNull { (name, isGranted) ->
            val rationalState = controller.shouldShowRational(name)
            val state = PermissionState.AndroidPermissionState(
                isGranted = isGranted,
                isShowRational = rationalState,
                isRequestedAtLeastOnce = true,
            )
            name.toAndroidPermission to state
        }.toMap()
    }

    private suspend fun markPermissionAsRequested(permissionName: String) {
        withContext(dispatchers.io) {
            prefs.edit { putBoolean(permissionName, true) }
        }
    }
}
