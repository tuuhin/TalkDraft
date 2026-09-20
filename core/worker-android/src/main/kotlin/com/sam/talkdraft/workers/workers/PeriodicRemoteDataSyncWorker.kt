package com.sam.talkdraft.workers.workers

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import co.touchlab.kermit.Logger
import com.sam.talkdraft.analytics.AnalyticsEvent
import com.sam.talkdraft.analytics.IAnalyticsProvider
import com.sam.talkdraft.model_manager.domain.repository.IUpdateTranscriptionModelRepo
import com.sam.talkdraft.notifications.NotificationConstants
import com.sam.talkdraft.workers.R
import org.koin.android.annotation.KoinWorker

@KoinWorker
class PeriodicRemoteDataSyncWorker internal constructor(
    workParams: WorkerParameters,
    private val context: Context,
    private val repo: IUpdateTranscriptionModelRepo,
    private val analytics: IAnalyticsProvider,
) : CoroutineWorker(context, workParams) {

    override suspend fun doWork(): Result {
        Logger.d(tag = TAG) { "START WORKER TO UPDATE DB MODEL DATA " }

        createAndShowForegroundInfo()

        Logger.d(tag = TAG) { "STARTING WORKER JOB" }
        analytics.track(AnalyticsEvent.ModelRemoteSyncStarted)

        val result = repo.syncLocalData()
        Logger.d(tag = TAG) { "WORKER JOB COMPLETED STATUS IS_SUCCESS:${result.isSuccess}" }
        return if (result.isSuccess) {
            analytics.track(
                AnalyticsEvent.ModelRemoteSyncSuccess,
                mapOf(WorkParams.DB_MODEL_SYNC_KEY to WorkParams.DB_MODEL_SYNC_SUCCESS),
            )
            Result.success(workDataOf(WorkParams.DB_MODEL_SYNC_KEY to WorkParams.DB_MODEL_SYNC_SUCCESS))
        } else {
            val err = result.exceptionOrNull()
            val message = err?.message ?: "SOME ERROR OCCURRED"
            analytics.track(
                AnalyticsEvent.ModelRemoteSyncFailed,
                mapOf(
                    WorkParams.DB_MODEL_SYNC_KEY to WorkParams.DB_MODEL_SYNC_FAILED,
                    WorkParams.DB_MODEL_SYNC_FAILED_REASON to message,
                ),
            )
            Result.failure(
                workDataOf(
                    WorkParams.DB_MODEL_SYNC_KEY to WorkParams.DB_MODEL_SYNC_FAILED,
                    WorkParams.DB_MODEL_SYNC_FAILED_REASON to message,
                ),
            )
        }
    }

    private fun createAndShowForegroundInfo() {
        val notification = createNotification()
        // foreground info to indicate something is going on
        val notificationID = NotificationConstants.LOCAL_DB_SYNC_WORKER_NOTIFICATION_ID
        try {
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                ForegroundInfo(notificationID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            else ForegroundInfo(notificationID, notification)
            // set foreground
            setForegroundAsync(info)
        } catch (e: Exception) {
            Logger.w(tag = TAG, throwable = e) { "FAILED TO SHOW FOREGROUND INFO" }
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(context, NotificationConstants.DB_SYNC_WORKER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sync)
            .setContentTitle(context.getString(R.string.sync_models_notification_title))
            .setProgress(100, 0, true)
            .setAutoCancel(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    companion object {
        private const val TAG = "PERIODIC_SYNC_MODEL_WORKER"
    }

}
