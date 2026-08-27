package com.sam.talkdraft.recorder.data

import com.sam.talkdraft.recorder.domain.IRecordPermissionChecker
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.cinterop.memScoped
import org.koin.core.annotation.Factory
import platform.AVFAudio.AVAudioApplication
import platform.AVFAudio.AVAudioApplicationRecordPermissionGranted
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.Foundation.NSOperatingSystemVersion
import platform.Foundation.NSProcessInfo

@Factory(binds = [IRecordPermissionChecker::class])
internal actual class RecordPermissionChecker : IRecordPermissionChecker {
    actual override fun hasPermission(): Boolean {
        if (isAtLeastIOS(17, 0)) {
            val status = AVAudioApplication.sharedInstance.recordPermission
            return status == AVAudioApplicationRecordPermissionGranted
        } else {
            return AVAudioSession.sharedInstance().recordPermission == AVAudioSessionRecordPermissionGranted
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
