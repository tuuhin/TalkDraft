package com.sam.talkdraft.permissions.controller

import androidx.activity.ComponentActivity

interface IPermissionController {

    fun bindToActivity(activity: ComponentActivity)

    fun isActivityBounded(): Boolean

    suspend fun requestPermission(permission: Array<String>): Map<String, @JvmSuppressWildcards Boolean>

    suspend fun shouldShowRational(permission: String): Boolean
}
