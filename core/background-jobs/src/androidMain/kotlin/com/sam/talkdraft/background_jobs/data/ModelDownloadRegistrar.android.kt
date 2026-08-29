package com.sam.talkdraft.background_jobs.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import co.touchlab.kermit.Logger
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
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import org.koin.core.annotation.Factory

private const val TAG = "MODEL_DOWNLOAD_REGISTRAR"

@Factory(binds = [IModelDownloadRegistrar::class])
actual class ModelDownloadRegistrar(private val context: Context) : IModelDownloadRegistrar {

    private val workManager by lazy { WorkManager.getInstance(context) }

    actual override fun startModelDownload(model: TranscriptionModel): Uuid {
        val workId = Uuid.random()
        val inputData = workDataOf(WorkParams.TRANSCRIPTION_MODEL_ID_INPUT_KEY to model.id.toString())

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

        workManager.enqueueUniqueWork(WORK_NAME_PREFIX + model.id, ExistingWorkPolicy.REPLACE, downloadRequest)

        Logger.d(tag = TAG) { "DOWNLOAD WORK CREATED :$workId " }
        return workId
    }

    actual override fun observerDownloadStatus(uuid: Uuid): Flow<ModelDownloadStatus> {
        return workManager.getWorkInfoByIdFlow(uuid.toJavaUuid())
            .onStart { Logger.d(tag = TAG) { "OBSERVING WORKER WITH ID :$uuid" } }
            .onCompletion { Logger.d(tag = TAG) { "OBSERVATION FINISHED WORK_ID:$uuid" } }
            .mapNotNull { workInfo ->
                if (workInfo == null) return@mapNotNull null
                mapWorkInfoToStatus(workInfo)
            }
    }

    actual override fun cancelDownload(uuid: Uuid) {
        Logger.d(tag = TAG) { "WORK CANCELLED :$uuid " }
        workManager.cancelWorkById(uuid.toJavaUuid())
    }

    private fun mapWorkInfoToStatus(workInfo: WorkInfo): ModelDownloadStatus? {
        val progressData = workInfo.progress
        val outputData = workInfo.outputData
        val statusKey = progressData.getString(WorkParams.TRANSCRIPTION_STATUS_KEY)

        return when (workInfo.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> {
                Logger.d(tag = TAG) { "WORKER ENQUEUE " }
                ModelDownloadStatus.DownloadInitiated
            }

            WorkInfo.State.RUNNING -> when (statusKey) {
                WorkParams.TRANSCRIPTION_STATUS_DOWNLOADING -> {
                    val percentage = progressData.getFloat(WorkParams.TRANSCRIPTION_STATUS_DOWNLOAD_PERCENTAGE, 0f)
                    ModelDownloadStatus.Downloading(percentage)
                }

                WorkParams.TRANSCRIPTION_STATUS_DOWNLOAD_SUCCESS -> ModelDownloadStatus.Success
                WorkParams.TRANSCRIPTION_STATUS_VERIFYING -> ModelDownloadStatus.Verifying
                WorkParams.TRANSCRIPTION_STATUS_DOWNLOAD_FAILED -> ModelDownloadStatus.Failed()
                WorkParams.TRANSCRIPTION_STATUS_STARTING_DOWNLOAD -> ModelDownloadStatus.DownloadInitiated
                else -> null
            }

            WorkInfo.State.SUCCEEDED -> {
                Logger.d(tag = TAG) { "WORKER SUCCESSFULLY COMPLETED" }
                ModelDownloadStatus.Success
            }

            WorkInfo.State.FAILED -> {
                val message = outputData.getString(WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_FAILED_REASON_MESSAGE)
                Logger.d(tag = TAG) { "WORKER FAILED :$message" }
                ModelDownloadStatus.Failed(message)
            }

            WorkInfo.State.CANCELLED -> {
                Logger.d(tag = TAG) { "WORKER WAS CANCELLED" }
                ModelDownloadStatus.Failed("Download Cancelled")
            }
        }
    }

    companion object {
        private const val WORK_NAME_PREFIX = "model_download_"
        private const val DOWNLOAD_WORK_TAG_PREFIX = "tag_model_download_"
    }
}
