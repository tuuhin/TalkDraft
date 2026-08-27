package com.sam.talkdraft.recorder.utils

import org.koin.core.annotation.Factory

@Factory
internal expect class GrantOrRequestPermissionRule {
    fun grantAudioPermission()
}
