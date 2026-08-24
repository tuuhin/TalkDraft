package com.sam.talkdraft.designsystem.utils.toast

import org.koin.core.annotation.Singleton

@Singleton
internal expect class PlatformToastProvider {
    suspend fun showToastMessage(message: String, mode: ToastDuration = ToastDuration.SHORT)
}
