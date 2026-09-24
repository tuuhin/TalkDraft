package com.sam.talkdraft.workers.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sam.talkdraft.analytics.AnalyticsEvent
import com.sam.talkdraft.analytics.IAnalyticsProvider
import com.sam.talkdraft.common.constants.IntentRequestCodes
import com.sam.talkdraft.model_downloader.domain.IModelDownloadManager
import com.sam.talkdraft.model_downloader.domain.models.DownloadState
import com.sam.talkdraft.model_downloader.domain.models.ModelDownloadStatus
import com.sam.talkdraft.notifications.NotificationConstants
import com.sam.talkdraft.workers.R
import com.sam.talkdraft.workers.utils.IWorkerEnvironment
import kotlin.math.roundToInt
import kotlin.uuid.Uuid
import org.koin.android.annotation.KoinWorker

@KoinWorker
class TranscriptionModuleDownloadWorker internal constructor(
    context: Context,
    private val params: WorkerParameters,
    private val downloader: IModelDownloadManager,
    private val analytics: IAnalyticsProvider,
    private val environment: IWorkerEnvironment,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val modelIdString = params.inputData.getString(WorkParams.TRANSCRIPTION_MODEL_ID_INPUT_KEY)
            ?: return Result.failure(
                workDataOf(
                    WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_FAILED to
                        WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_FAILED_REASON_INVALID_ID,
                ),
            )

        val modelId = try {
            Uuid.parse(modelIdString)
        } catch (_: IllegalArgumentException) {
            analytics.track(
                AnalyticsEvent.ModelRemoteSyncFailed,
                mapOf(
                    WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_FAILED_REASON_MESSAGE to
                        WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_FAILED_REASON_INVALID_ID,
                ),
            )
            return Result.failure(
                workDataOf(
                    WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_FAILED to
                        WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_FAILED_REASON_INVALID_ID,
                ),
            )
        }

        if (environment.isProd)
            setForegroundAsync(createNotification(DownloadState.Initiated))

        val taskResult = downloader.downloadAndSaveModel(
            modelId = modelId,
            onDownloadState = { state ->
                // Update worker intermediate progress state
                setProgressAsync(state.toWorkData())
                // Safely post foreground notification update
                if (environment.isProd) setForegroundAsync(createNotification(state.state))
            },
        )

        if (taskResult.isFailure) {
            val reason = taskResult.exceptionOrNull()?.message ?: "Unknown download failure"
            analytics.track(
                AnalyticsEvent.ModelDownloadFailed,
                mapOf(
                    WorkParams.TRANSCRIPTION_MODEL_ID_INPUT_KEY to modelId,
                    WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_KEY to WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_FAILED,
                    WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_FAILED_REASON_MESSAGE to reason,
                ),
            )
            return Result.failure(
                workDataOf(WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_KEY to WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_FAILED),
            )
        }

        val isSuccess = taskResult.getOrNull() ?: false
        analytics.track(
            AnalyticsEvent.ModelDownloadCompleted,
            mapOf(
                WorkParams.TRANSCRIPTION_MODEL_ID_INPUT_KEY to modelId,
                WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_KEY to WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_SUCCESS,
                WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_SAVE_STATUS to isSuccess,
            ),
        )

        // show a completion notification with a done sound
        if (environment.isProd && isSuccess) showCompleteNotification()

        return Result.success(
            workDataOf(WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_KEY to WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_SUCCESS),
        )
    }

    private fun ModelDownloadStatus.toWorkData(): Data {
        val builder = Data.Builder()
        val stateValue = state
        builder.putString(WorkParams.TRANSCRIPTION_STATUS_MODEL_ID_KEY, modelId.toString())

        val statusKey = when (state) {
            is DownloadState.Downloading -> WorkParams.TRANSCRIPTION_STATUS_DOWNLOADING
            DownloadState.Extracting -> WorkParams.TRANSCRIPTION_STATUS_EXTRACTING
            is DownloadState.Failed -> WorkParams.TRANSCRIPTION_STATUS_DOWNLOAD_FAILED
            DownloadState.Initiated -> WorkParams.TRANSCRIPTION_STATUS_STARTING_DOWNLOAD
            DownloadState.Success -> WorkParams.TRANSCRIPTION_STATUS_DOWNLOAD_SUCCESS
            DownloadState.Verifying -> WorkParams.TRANSCRIPTION_STATUS_VERIFYING
        }

        builder.putString(WorkParams.TRANSCRIPTION_STATUS_KEY, statusKey)
        if (stateValue is DownloadState.Downloading) {
            builder.putFloat(WorkParams.TRANSCRIPTION_STATUS_DOWNLOAD_PERCENTAGE, stateValue.progress)
        } else if (stateValue is DownloadState.Failed) {
            builder.putString(WorkParams.TRANSCRIPTION_STATUS_DOWNLOAD_FAILED_REASON, stateValue.message ?: "")
        }
        return builder.build()
    }


    private fun createNotification(state: DownloadState): ForegroundInfo {
        val title = applicationContext.getString(R.string.downloading_speech_model_notification_title)
        val isEndState = state is DownloadState.Success || state is DownloadState.Failed

        // add the content intent
        val packageName = applicationContext.packageName
        val mainActivityClassName = "$packageName.MainActivity"

        val intent = Intent().apply {
            component = ComponentName(packageName, mainActivityClassName)
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val pendingIntent = PendingIntent
            .getActivity(applicationContext, IntentRequestCodes.OPEN_ONGOING_MODEL_DOWNLOAD, intent, flags)

        val textResource = when (state) {
            is DownloadState.Downloading -> R.string.downloading_speech_model_notification_body
            is DownloadState.Failed -> R.string.failed_speech_model_download_notification_body
            DownloadState.Initiated -> R.string.start_download_notification_body
            DownloadState.Success -> R.string.speech_model_notification_download_success
            DownloadState.Verifying -> R.string.verifying_speech_model_notification_body
            DownloadState.Extracting -> R.string.extracting_speech_model_notification_body
        }

        val builder = NotificationCompat.Builder(
            applicationContext,
            NotificationConstants.DOWNLOAD_MODEL_WORKER_CHANNEL_ID,
        )
            .setContentTitle(title)
            .setContentText(applicationContext.getString(textResource))
            .setSmallIcon(R.drawable.ic_download_simplified)
            .setOngoing(!isEndState)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (!isEndState) {
            val intent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
            val cancelAction = NotificationCompat.Action.Builder(
                IconCompat.createWithResource(applicationContext, R.drawable.ic_cancel),
                "Cancel",
                intent,
            ).build()
            builder.addAction(cancelAction)
        }

        when (state) {
            is DownloadState.Downloading -> {
                val progress = state.progress.roundToInt().coerceAtMost(100)
                builder.setProgress(100, progress, false)
            }

            DownloadState.Verifying -> builder.setProgress(100, 0, true)
            else -> builder.setProgress(0, 0, false)
        }

        val notification = builder.build()
        val notificationId = NotificationConstants.DOWNLOAD_STT_MODEL_WORKER_NOTIFICATION_ID

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else ForegroundInfo(notificationId, notification)
    }

    private fun showCompleteNotification() {
        val notificationManager = applicationContext.getSystemService<NotificationManager>() ?: return
        val soundUri =
            "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${applicationContext.packageName}/${R.raw.sound_of_success}".toUri()

        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationConstants.DOWNLOAD_MODEL_WORKER_CHANNEL_ID,
        )
            .setContentTitle(applicationContext.getString(R.string.speech_model_notification_download_success_title))
            .setContentText(applicationContext.getString(R.string.speech_model_notification_download_success))
            .setSmallIcon(R.drawable.ic_download_success)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri, AudioManager.STREAM_NOTIFICATION)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

        notificationManager.notify(NotificationConstants.DOWNLOAD_STT_MODEL_WORKER_NOTIFICATION_ID, notification)
    }
}
