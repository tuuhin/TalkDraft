package com.sam.talkdraft.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import org.koin.core.annotation.Singleton

@Singleton(binds = [INotificationChannelRegistrar::class])
internal class NotificationChannelRegistrarImpl(
    private val context: Context,
) : INotificationChannelRegistrar {

    private val notificationManager by lazy { context.getSystemService<NotificationManager>() }

    override fun registerChannels() {
        createDbSyncChannel()
    }

    private fun createDbSyncChannel() {
        val channel = NotificationChannel(
            NotificationConstants.DB_SYNC_WORKER_CHANNEL_ID, NotificationConstants.DB_SYNC_WORKER_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            description = NotificationConstants.DB_SYNC_WORKER_CHANNEL_DESCRIPTION
        }
        notificationManager?.createNotificationChannel(channel)
    }
}
