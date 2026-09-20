package com.sam.talkdraft.background_jobs.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import co.touchlab.kermit.Logger
import com.sam.talkdraft.background_jobs.IModelDownloadRegistrar
import com.sam.talkdraft.model_downloader.domain.models.DownloadState
import com.sam.talkdraft.model_downloader.domain.models.ModelDownloadStatus
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import com.sam.talkdraft.workers.workers.TranscriptionModuleDownloadWorker
import com.sam.talkdraft.workers.workers.WorkParams
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
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
            .build()

        val downloadRequest = OneTimeWorkRequestBuilder<TranscriptionModuleDownloadWorker>()
            .setId(workId.toJavaUuid())
            .setInputData(inputData)
            .setConstraints(constraints)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(DOWNLOAD_WORK_TAG_PREFIX + model.id)
            .addTag(DOWNLOAD_WORKER_TAG)
            .build()

        val workerName = WORK_NAME_PREFIX + model.id
        val op = workManager.enqueueUniqueWork(workerName, ExistingWorkPolicy.REPLACE, downloadRequest)

        Logger.d(tag = TAG) { "DOWNLOAD WORK CREATED :$workId OPERATION:${op.state.value}" }
        return downloadRequest.id.toKotlinUuid()
    }

    actual override fun observerDownloadStatus(uuid: Uuid): Flow<ModelDownloadStatus> {
        return workManager.getWorkInfoByIdFlow(uuid.toJavaUuid())
            .onStart { Logger.d(tag = TAG) { "OBSERVING WORKER WITH ID :$uuid" } }
            .onCompletion { Logger.d(tag = TAG) { "OBSERVATION FINISHED WORK_ID:$uuid" } }
            .filterNotNull()
            .mapNotNull { workInfo -> mapWorkInfoToStatus(workInfo, fallbackModelId = null) }
            .distinctUntilChanged()
    }

    actual override fun observerDownloadStatus(model: TranscriptionModel): Flow<Pair<Uuid, ModelDownloadStatus>> {
        return workManager.getWorkInfosByTagFlow(DOWNLOAD_WORKER_TAG)
            .onStart { Logger.d(tag = TAG) { "OBSERVING WORKER WITH WORKER TAG" } }
            .onCompletion { Logger.d(tag = TAG) { "OBSERVATION FINISHED WORKER TAG" } }
            .mapNotNull { workInfos ->
                val workInfo = workInfos.firstOrNull() ?: return@mapNotNull null
                val status = mapWorkInfoToStatus(workInfo, fallbackModelId = model.id) ?: return@mapNotNull null
                workInfo.id.toKotlinUuid() to status
            }
            .distinctUntilChanged()
    }

    actual override fun cancelDownload(uuid: Uuid) {
        Logger.d(tag = TAG) { "WORK CANCELLED :$uuid " }
        workManager.cancelWorkById(uuid.toJavaUuid())
    }

    private fun mapWorkInfoToStatus(
        workInfo: WorkInfo,
        fallbackModelId: Uuid? = null,
    ): ModelDownloadStatus? {

        Logger.d(tag = TAG) { "WORK INFO: ID:${workInfo.id} STOP_REASON:${workInfo.stopReason}" }

        val progressData = workInfo.progress
        val outputData = workInfo.outputData

        // extract the id from the tag itself
        // fallback to progress data and output data
        val modelIdString = workInfo.tags
            .firstOrNull { it.startsWith(DOWNLOAD_WORK_TAG_PREFIX) }
            ?.removePrefix(DOWNLOAD_WORK_TAG_PREFIX)
            ?: progressData.getString(WorkParams.TRANSCRIPTION_STATUS_MODEL_ID_KEY)
            ?: outputData.getString(WorkParams.TRANSCRIPTION_STATUS_MODEL_ID_KEY)

        val modelId = modelIdString?.let {
            try {
                Uuid.parse(it)
            } catch (_: Exception) {
                Logger.d(tag = TAG) { "FAILED TO READ THE ID FROM MODEL ID STRING" }
                null
            }
        } ?: fallbackModelId

        if (modelId == null) {
            Logger.w(tag = TAG) { "Could not determine model ID for work request ${workInfo.id}" }
            return null
        }

        val statusKey = progressData.getString(WorkParams.TRANSCRIPTION_STATUS_KEY)

        if (workInfo.state != WorkInfo.State.RUNNING) {
            Logger.d(tag = TAG) { "CURRENT WORKER STATE :${workInfo.state}" }
        }

        val state: DownloadState = when (workInfo.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DownloadState.Initiated
            WorkInfo.State.RUNNING -> when (statusKey) {
                WorkParams.TRANSCRIPTION_STATUS_STARTING_DOWNLOAD -> DownloadState.Initiated
                WorkParams.TRANSCRIPTION_STATUS_DOWNLOADING -> {
                    val percentage = progressData.getFloat(WorkParams.TRANSCRIPTION_STATUS_DOWNLOAD_PERCENTAGE, 0f)
                    DownloadState.Downloading(percentage)
                }

                WorkParams.TRANSCRIPTION_STATUS_VERIFYING -> DownloadState.Verifying
                WorkParams.TRANSCRIPTION_STATUS_DOWNLOAD_SUCCESS -> DownloadState.Success
                WorkParams.TRANSCRIPTION_STATUS_DOWNLOAD_FAILED -> DownloadState.Failed()
                else -> DownloadState.Initiated
            }

            WorkInfo.State.SUCCEEDED -> DownloadState.Success
            WorkInfo.State.CANCELLED -> DownloadState.Failed("Download Cancelled")

            WorkInfo.State.FAILED -> {
                val message = outputData.getString(WorkParams.TRANSCRIPTION_MODEL_DOWNLOAD_FAILED_REASON_MESSAGE)
                DownloadState.Failed(message)
            }
        }
        return ModelDownloadStatus(modelId = modelId, state = state)
    }

    companion object {
        private const val WORK_NAME_PREFIX = "model_download_"
        private const val DOWNLOAD_WORK_TAG_PREFIX = "tag_model_download_"
        private const val DOWNLOAD_WORKER_TAG = "model_downloader_tag"
    }
}
