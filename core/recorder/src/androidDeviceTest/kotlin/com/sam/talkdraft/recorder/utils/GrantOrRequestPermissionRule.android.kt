package com.sam.talkdraft.recorder.utils

import android.Manifest
import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.koin.core.annotation.Factory

@Factory
internal actual class GrantOrRequestPermissionRule(private val context: Context) {
    actual fun grantAudioPermission() {
        val packageName = context.packageName
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.grantRuntimePermission(packageName, Manifest.permission.RECORD_AUDIO)
    }
}
