package com.sam.talkdraft.designsystem.utils.toast

import android.content.Context
import android.widget.Toast
import org.koin.core.annotation.Singleton

@Singleton
internal actual class PlatformToastProvider(private val context: Context) {

    actual suspend fun showToastMessage(message: String, mode: ToastDuration) {
        val duration = when (mode) {
            ToastDuration.SHORT -> Toast.LENGTH_SHORT
            ToastDuration.LONG -> Toast.LENGTH_LONG
        }

        Toast.makeText(context, message, duration).show()
    }
}
