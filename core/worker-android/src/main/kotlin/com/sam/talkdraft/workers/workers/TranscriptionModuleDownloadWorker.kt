package com.sam.talkdraft.workers.workers

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.sam.talkdraft.analytics.AnalyticsEvent
import com.sam.talkdraft.analytics.IAnalyticsProvider
import com.sam.talkdraft.model_downloader.domain.IModelDownloadManager
import com.sam.talkdraft.model_downloader.domain.models.ModelDownloadStatus
import com.sam.talkdraft.notifications.NotificationConstants
import com.sam.talkdraft.workers.R
import com.sam.talkdraft.workers.utils.IWorkerEnvironment
import kotlin.uuid.Uuid
import org.koin.android.annotation.KoinWorker

@KoinWorker
internal class TranscriptionModuleDownloadWorker(
    context: Context,
    private val params: WorkerParameters,
    private val downloader: IModelDownloadManager,
    private val analytics: IAnalyticsProvider,
    private val environment: IWorkerEnvironment,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val modelIdString = params.inputData.getString(WorkParams.TRANSCRIPTION_INPUT_MODEL_ID)
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
            setForegroundAsync(createNotification(ModelDownloadStatus.DownloadInitiated))

        val taskResult = downloader.downloadAndSaveModel(
            modelId = modelId,
            onDownloadState = { state ->
                // Update worker intermediate progress state
                setProgressAsync(getWorkDataForState(state))
                // Safely post foreground notification update
                if (environment.isProd)
                    setForegroundAsync(createNotification(state))
            },
        )

        if (taskResult.isFailure) {
            val reason = taskResult.exceptionOrNull()?.message ?: "Unknown download failure"
            analytics.track(
                AnalyticsEvent.ModelDownloadFailed,
                mapOf(
                    WorkParams.TRANSCRIPTION_INPUT_MODEL_ID to modelId,
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
                WorkParams.TRANSCRIPTION_INPUT_MODEL_ID to modelId,
                WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_KEY to WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_SUCCESS,
                WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_SAVE_STATUS to isSuccess,
            ),
        )

        return Result.success(
            workDataOf(WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_KEY to WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_SUCCESS),
        )
    }

    private fun getWorkDataForState(state: ModelDownloadStatus) = when (state) {
        ModelDownloadStatus.DownloadInitiated -> workDataOf(
            WorkParams.TRANSCRIPTION_STATUS_KEY to WorkParams.TRANSCRIPTION_STATUS_STARTING_DOWNLOAD,
        )

        is ModelDownloadStatus.Downloading -> workDataOf(
            WorkParams.TRANSCRIPTION_STATUS_KEY to WorkParams.TRANSCRIPTION_STATUS_DOWNLOADING,
            WorkParams.TRANSCRIPTION_STATUS_DOWNLOAD_PERCENTAGE to state.percentage,
        )

        ModelDownloadStatus.Failed -> workDataOf(
            WorkParams.TRANSCRIPTION_STATUS_KEY to WorkParams.TRANCRIPTION_STATUS_DOWNLOAD_FAILED,
        )

        ModelDownloadStatus.Success -> workDataOf(
            WorkParams.TRANSCRIPTION_STATUS_KEY to WorkParams.TRANSCRIPTION_STATUS_DOWNLOAD_SUCCESS,
        )

        ModelDownloadStatus.Verifying -> workDataOf(
            WorkParams.TRANSCRIPTION_STATUS_KEY to WorkParams.TRANSCRIPTION_STATUS_VERIFYING,
        )
    }

    private fun createNotification(state: ModelDownloadStatus): ForegroundInfo {
        val title = applicationContext.getString(R.string.downloading_speech_model_notification_title)
        val isTerminalState = state is ModelDownloadStatus.Success || state is ModelDownloadStatus.Failed

        val textResource = when (state) {
            ModelDownloadStatus.DownloadInitiated -> R.string.start_download_notification_body
            is ModelDownloadStatus.Downloading -> R.string.downloading_speech_model_notification_body
            ModelDownloadStatus.Failed -> R.string.failed_speech_model_download_notification_body
            ModelDownloadStatus.Success -> R.string.speech_model_notification_download_success
            ModelDownloadStatus.Verifying -> R.string.verifying_speech_model_notification_body
        }

        val builder =
            NotificationCompat.Builder(applicationContext, NotificationConstants.DOWNLOAD_MODEL_WORKER_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(applicationContext.getString(textResource))
                .setSmallIcon(R.drawable.ic_cloud_download)
                .setOngoing(!isTerminalState)
                .setSilent(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)

        if (!isTerminalState) {
            val intent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
            val cancelAction = NotificationCompat.Action.Builder(
                IconCompat.createWithResource(applicationContext, R.drawable.ic_cancel),
                "Cancel",
                intent,
            ).build()
            builder.addAction(cancelAction)
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }

        when (state) {
            is ModelDownloadStatus.Downloading -> builder.setProgress(100, state.percentage, false)
            ModelDownloadStatus.Verifying -> builder.setProgress(100, 0, true)
            else -> builder.setProgress(0, 0, false)
        }

        val notification = builder.build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ForegroundInfo(
                NotificationConstants.DOWNLOAD_STT_MODEL_WORKER_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(
                NotificationConstants.DOWNLOAD_STT_MODEL_WORKER_NOTIFICATION_ID,
                notification,
            )
        }
    }
}
