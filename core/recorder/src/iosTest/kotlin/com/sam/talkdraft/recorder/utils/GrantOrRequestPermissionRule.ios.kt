package com.sam.talkdraft.recorder.utils

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.memScoped
import org.koin.core.annotation.Factory
import platform.AVFAudio.AVAudioApplication
import platform.AVFAudio.AVAudioSession
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.authorizationStatusForMediaType
import platform.Foundation.NSOperatingSystemVersion
import platform.Foundation.NSProcessInfo

private const val TAG = "GrantRequestPermissionRule"

@Factory
internal actual class GrantOrRequestPermissionRule {
    actual fun grantAudioPermission() {
        val currentStatus = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeAudio)
        if (currentStatus == AVAuthorizationStatusAuthorized) return

        if (isAtLeastIOS(17, 0)) {
            AVAudioApplication.requestRecordPermissionWithCompletionHandler { granted ->
                if (!granted) Logger.d(tag = TAG) { "FAILED TO REQUEST PERMISSIONS" }

            }
        } else AVAudioSession.sharedInstance().requestRecordPermission { granted ->
            if (!granted) Logger.d(tag = TAG) { "FAILED TO REQUEST PERMISSIONS" }
        }

    }

    @OptIn(ExperimentalForeignApi::class)
    fun isAtLeastIOS(major: Long, minor: Long = 0, patch: Long = 0): Boolean = memScoped {
        val version = cValue<NSOperatingSystemVersion> {
            majorVersion = major
            minorVersion = minor
            patchVersion = patch
        }
        return NSProcessInfo.processInfo.isOperatingSystemAtLeastVersion(version)
    }
}
