package com.sam.talkdraft.permissions

import android.Manifest
import android.os.Build
import com.sam.talkdraft.permissions.model.Permissions

internal val Permissions.toPermissionName: String?
    get() = when (this) {
        Permissions.NOTIFICATION -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.POST_NOTIFICATIONS else null
        Permissions.RECORD_AUDIO -> Manifest.permission.RECORD_AUDIO
    }

internal val String.toAndroidPermission: Permissions
    get() = when (this) {
        Manifest.permission.POST_NOTIFICATIONS -> Permissions.NOTIFICATION
        Manifest.permission.RECORD_AUDIO -> Permissions.RECORD_AUDIO
        else -> throw IllegalArgumentException("Permission is not supported for now")
    }
