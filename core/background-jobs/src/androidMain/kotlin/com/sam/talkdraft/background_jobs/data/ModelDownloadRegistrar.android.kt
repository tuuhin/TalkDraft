package com.sam.talkdraft.background_jobs.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.sam.talkdraft.background_jobs.IModelDownloadRegistrar
import com.sam.talkdraft.model_downloader.domain.models.ModelDownloadStatus
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import com.sam.talkdraft.workers.workers.TranscriptionModuleDownloadWorker
import com.sam.talkdraft.workers.workers.WorkParams
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import org.koin.core.annotation.Factory

@Factory(binds = [IModelDownloadRegistrar::class])
actual class ModelDownloadRegistrar(private val context: Context) : IModelDownloadRegistrar {

    actual override fun startModelDownload(model: TranscriptionModel): Uuid {
        val workId = Uuid.random()
        val inputData = workDataOf(WorkParams.TRANSCRIPTION_INPUT_MODEL_ID to model.id.toString())

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresStorageNotLow(false)
            .build()

        val downloadRequest = OneTimeWorkRequestBuilder<TranscriptionModuleDownloadWorker>()
            .setId(workId.toJavaUuid())
            .setInputData(inputData)
            .setConstraints(constraints)
            .setInitialDelay(2.seconds.toJavaDuration())
            .addTag(DOWNLOAD_WORK_TAG_PREFIX + model.id)
            .build()

        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(WORK_NAME_PREFIX + model.id, ExistingWorkPolicy.REPLACE, downloadRequest)

        return workId
    }

    actual override fun observerDownloadStatus(uuid: Uuid): Flow<ModelDownloadStatus> {
        val workManager = WorkManager.getInstance(context)
        return workManager.getWorkInfoByIdFlow(uuid.toJavaUuid())
            .mapNotNull { workInfo ->
                if (workInfo == null) return@mapNotNull null
                mapWorkInfoToStatus(workInfo)
            }
    }

    actual override fun cancelDownload(uuid: Uuid) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelWorkById(uuid.toJavaUuid())
    }

    private fun mapWorkInfoToStatus(workInfo: WorkInfo): ModelDownloadStatus {
        val progressData = workInfo.progress
        val outputData = workInfo.outputData

        val statusKey = progressData.getString(WorkParams.TRANSCRIPTION_STATUS_KEY)
            ?: outputData.getString(WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_KEY)

        return when (workInfo.state) {
            WorkInfo.State.ENQUEUED -> ModelDownloadStatus.DownloadInitiated
            WorkInfo.State.RUNNING -> {
                when (statusKey) {
                    WorkParams.TRANSCRIPTION_STATUS_DOWNLOADING -> {
                        val percentage = progressData.getInt(WorkParams.TRANSCRIPTION_STATUS_DOWNLOAD_PERCENTAGE, 0)
                        ModelDownloadStatus.Downloading(percentage)
                    }

                    WorkParams.TRANSCRIPTION_STATUS_VERIFYING -> ModelDownloadStatus.Verifying
                    else -> ModelDownloadStatus.DownloadInitiated
                }
            }

            WorkInfo.State.SUCCEEDED -> ModelDownloadStatus.Success
            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> ModelDownloadStatus.Failed
            WorkInfo.State.BLOCKED -> ModelDownloadStatus.DownloadInitiated
        }
    }

    companion object {
        private const val WORK_NAME_PREFIX = "model_download_"
        private const val DOWNLOAD_WORK_TAG_PREFIX = "tag_model_download_"
    }
}
