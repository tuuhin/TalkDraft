package com.sam.talkdraft.designsystem.utils.toast

import org.koin.core.annotation.Singleton

@Singleton
internal actual class PlatformToastProvider {
    actual suspend fun showToastMessage(message: String, mode: ToastDuration) {
    }
}
