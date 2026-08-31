package com.sam.talkdraft.permissions

import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.permissions.model.IosPermissionStatus
import com.sam.talkdraft.permissions.model.PermissionState
import com.sam.talkdraft.permissions.model.Permissions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import platform.AVFAudio.AVAudioApplication
import platform.AVFAudio.AVAudioApplicationRecordPermissionDenied
import platform.AVFAudio.AVAudioApplicationRecordPermissionGranted
import platform.AVFAudio.AVAudioApplicationRecordPermissionUndetermined
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionRecordPermissionDenied
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFAudio.AVAudioSessionRecordPermissionUndetermined
import platform.Foundation.NSOperatingSystemVersion
import platform.Foundation.NSProcessInfo
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter

@OptIn(ExperimentalForeignApi::class)
@Factory(binds = [IPermissionsRequester::class])
internal actual class PermissionsRequesterImpl(
    private val dispatcher: IPlatformCoroutineDispatchers,
) : IPermissionsRequester {

    actual override suspend fun checkPermissionStatus(permission: Permissions): PermissionState {
        return withContext(dispatcher.mainImmediate) {
            val status = when (permission) {
                Permissions.NOTIFICATION -> checkNotificationPermission()
                Permissions.RECORD_AUDIO -> checkRecordAudioPermission()
            }
            PermissionState.IosPermissionState(status)
        }
    }

    actual override suspend fun requestPermission(permission: Permissions): PermissionState {
        return withContext(dispatcher.mainImmediate) {
            val iosState = when (permission) {
                Permissions.NOTIFICATION -> requestNotificationPermission()
                Permissions.RECORD_AUDIO -> requestAudioPermission()
            }
            PermissionState.IosPermissionState(iosState)
        }
    }

    actual override suspend fun requestPermissions(permissions: List<Permissions>): Map<Permissions, PermissionState> {
        return withContext(dispatcher.mainImmediate) {
            permissions.toSet().associateWith {
                val iosState = when (it) {
                    Permissions.NOTIFICATION -> requestNotificationPermission()
                    Permissions.RECORD_AUDIO -> requestAudioPermission()
                }
                PermissionState.IosPermissionState(iosState)
            }
        }
    }


    private suspend fun checkNotificationPermission(): IosPermissionStatus = suspendCancellableCoroutine { cont ->
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val state = when (settings?.authorizationStatus) {
                UNAuthorizationStatusAuthorized, UNAuthorizationStatusProvisional -> IosPermissionStatus.GRANTED
                UNAuthorizationStatusDenied -> IosPermissionStatus.DENIED
                UNAuthorizationStatusNotDetermined -> IosPermissionStatus.NOT_DETERMINED
                else -> IosPermissionStatus.NOT_DETERMINED
            }
            if (cont.isActive) cont.resume(state)
        }
    }


    private suspend fun requestNotificationPermission(): IosPermissionStatus = suspendCancellableCoroutine { cont ->
        val center = UNUserNotificationCenter.currentNotificationCenter()
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge

        center.requestAuthorizationWithOptions(options) { granted, error ->
            if (error != null) {
                cont.resumeWithException(Exception(error.localizedDescription))
                return@requestAuthorizationWithOptions
            }
            val state = if (granted) IosPermissionStatus.GRANTED else IosPermissionStatus.DENIED
            if (cont.isActive) cont.resume(state)
        }
    }


    private suspend fun checkRecordAudioPermission(): IosPermissionStatus = suspendCancellableCoroutine { cont ->
        val permissionStatus = if (isAtLeastIOS17()) when (AVAudioApplication.sharedInstance().recordPermission) {
            AVAudioApplicationRecordPermissionGranted -> IosPermissionStatus.GRANTED
            AVAudioApplicationRecordPermissionDenied -> IosPermissionStatus.DENIED
            AVAudioApplicationRecordPermissionUndetermined -> IosPermissionStatus.NOT_DETERMINED
            else -> IosPermissionStatus.NOT_DETERMINED
        }
        else when (AVAudioSession.sharedInstance().recordPermission) {
            AVAudioSessionRecordPermissionGranted -> IosPermissionStatus.GRANTED
            AVAudioSessionRecordPermissionDenied -> IosPermissionStatus.DENIED
            AVAudioSessionRecordPermissionUndetermined -> IosPermissionStatus.NOT_DETERMINED
            else -> IosPermissionStatus.NOT_DETERMINED
        }
        if (cont.isActive) cont.resume(permissionStatus)
    }

    private suspend fun requestAudioPermission(): IosPermissionStatus = suspendCancellableCoroutine { cont ->
        if (isAtLeastIOS17()) AVAudioApplication.requestRecordPermissionWithCompletionHandler { granted ->
            val state = if (granted) IosPermissionStatus.GRANTED else IosPermissionStatus.DENIED
            if (cont.isActive) cont.resume(state)
        }
        else AVAudioSession.sharedInstance().requestRecordPermission { granted ->
            val state = if (granted) IosPermissionStatus.GRANTED else IosPermissionStatus.DENIED
            if (cont.isActive) cont.resume(state)
        }
    }


    private fun isAtLeastIOS17(): Boolean {
        val version = cValue<NSOperatingSystemVersion> {
            majorVersion = 17
            minorVersion = 0
            patchVersion = 0
        }
        return NSProcessInfo.processInfo.isOperatingSystemAtLeastVersion(version)
    }
}
