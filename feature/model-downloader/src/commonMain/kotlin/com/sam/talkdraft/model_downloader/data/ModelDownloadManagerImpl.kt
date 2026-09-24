package com.sam.talkdraft.model_downloader.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.model_downloader.domain.IDownloadTempFileManager
import com.sam.talkdraft.model_downloader.domain.IModelDownloadManager
import com.sam.talkdraft.model_downloader.domain.IModelDownloadVerifier
import com.sam.talkdraft.model_downloader.domain.IModelFileManager
import com.sam.talkdraft.model_downloader.domain.exceptions.ModelDownloadFailedException
import com.sam.talkdraft.model_downloader.domain.exceptions.ModelVerificationFailedException
import com.sam.talkdraft.model_downloader.domain.models.DownloadState
import com.sam.talkdraft.model_downloader.domain.models.ModelDownloadStatus
import com.sam.talkdraft.model_manager.domain.model.ModelInstallStatus
import com.sam.talkdraft.model_manager.domain.model.TranscriberFamily
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import com.sam.talkdraft.model_manager.domain.repository.ISelectedTranscriptionModelStore
import com.sam.talkdraft.model_manager.domain.repository.ITranscriptionModelsRepo
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import okio.Path
import org.koin.core.annotation.Factory

private const val TAG = "MODEL_DOWNLOAD_MANAGER"

@Factory(binds = [IModelDownloadManager::class])
internal class ModelDownloadManagerImpl(
    private val modelFileManager: IModelFileManager,
    private val tempFileManage: IDownloadTempFileManager,
    private val verifier: IModelDownloadVerifier,
    private val httpclient: HttpClient,
    private val repository: ITranscriptionModelsRepo,
    private val selectedModelStore: ISelectedTranscriptionModelStore,
) : IModelDownloadManager {

    override suspend fun downloadAndSaveModel(
        modelId: Uuid,
        onDownloadState: suspend (ModelDownloadStatus) -> Unit,
    ): Result<Boolean> {

        val modelResult = repository.readModel(modelId)
        if (modelResult.isFailure) {
            val ex = modelResult.exceptionOrNull() ?: Exception("Invalid result")
            return Result.failure(ex)
        }

        val transcriptionModel = modelResult.getOrThrow()
        val downloadURI = transcriptionModel.downloadURL ?: return Result.failure(Exception("Invalid download url"))

        // Helper to emit states concisely
        suspend fun emitState(state: DownloadState) {
            onDownloadState(ModelDownloadStatus(modelId = modelId, state = state))
        }

        // Update model state to downloading
        repository.updateModelStatus(transcriptionModel.id, ModelInstallStatus.DOWNLOADING)
        emitState(DownloadState.Initiated)

        var tempFilePath: Path? = null

        return try {
            // Download the file into temp cache
            var prevProgress = -1
            val downloadResult = downloadModelFile(transcriptionModel, downloadURI) { progress ->
                val progressPercentage = (progress * 100).toInt()
                if (progressPercentage > prevProgress) {
                    prevProgress = progressPercentage
                    Logger.d(tag = TAG) { "DOWNLOAD PROGRESS: $progressPercentage%" }
                    emitState(DownloadState.Downloading(prevProgress.toFloat()))
                }
            }

            // we have a temp file path
            tempFilePath = downloadResult.fold(
                onSuccess = { it },
                onFailure = { exc ->
                    // download failed
                    Logger.d(tag = TAG) { "FAILED TO DOWNLOAD THE MODEL" }
                    withContext(NonCancellable) {
                        // update model state to not installed as it's a failed download
                        repository.updateModelStatus(transcriptionModel.id, ModelInstallStatus.NOT_INSTALLED)
                    }
                    emitState(DownloadState.Failed(exc.message))
                    return Result.failure(exc)
                },
            )

            // start verification step if we not  need verification this will end quickly
            emitState(DownloadState.Verifying)
            val isVerified = verifier.validateModelHash(tempFilePath, transcriptionModel)
            if (!isVerified) {
                Logger.e(tag = TAG) { "MODEL HASH VERIFICATION FAILED" }
                withContext(NonCancellable) {
                    // update model state to not installed as it's a failed download
                    repository.updateModelStatus(transcriptionModel.id, ModelInstallStatus.NOT_INSTALLED)
                    // delete the temporary downloaded model
                    tempFileManage.clearCache(tempFilePath)
                }
                emitState(DownloadState.Failed("Verification failed"))
                return Result.failure(ModelVerificationFailedException())
            }

            // marking an extracting step here and final save to model
            emitState(DownloadState.Extracting)
            val saveResult = modelFileManager.saveModel(transcriptionModel, tempFilePath)
            saveResult.fold(
                onSuccess = {
                    selectedModelStore.setSelectedModel(transcriptionModel)
                    emitState(DownloadState.Success)
                    Result.success(true)
                },
                onFailure = { error ->
                    Logger.e(tag = TAG, throwable = error) { "FAILED TO SAVE MODEL FILE" }
                    withContext(NonCancellable) {
                        tempFileManage.clearCache(tempFilePath)
                    }
                    emitState(DownloadState.Failed(error.message))
                    return Result.failure(error)
                },
            )
        } catch (e: CancellationException) {
            Logger.w(tag = TAG) { "OPERATION CANCELLED" }
            throw e
        } catch (e: Exception) {
            Logger.e(tag = TAG, throwable = e) { "FAILED TO PERFORM DOWNLOAD" }
            emitState(DownloadState.Failed(e.message))
            Result.failure(e)
        } finally {
            withContext(NonCancellable) {
                val currentModel = repository.readModel(modelId).getOrNull()
                if (currentModel?.status != ModelInstallStatus.INSTALLED) {
                    Logger.d(tag = TAG) { "RESETTING THE MODEL AS STATUS IS NOT INSTALLED" }
                    repository.updateModelStatus(transcriptionModel.id, ModelInstallStatus.NOT_INSTALLED)
                }
                tempFilePath?.let { path -> tempFileManage.clearCache(path) }
            }
        }
    }

    private suspend fun downloadModelFile(
        model: TranscriptionModel,
        downloadURL: String,
        onProgress: suspend (Float) -> Unit = {},
    ): Result<Path> = try {
        var showTotalBytes = false
        val statement = httpclient.prepareGet(downloadURL) {
            url {
                if (model.modelFamily == TranscriberFamily.WHISPER)
                    parameters.append("download", "true")
            }
            onDownload { readBytes, totalBytes ->
                val total = totalBytes ?: 0
                if (total > 0) {
                    if (!showTotalBytes) {
                        Logger.i(tag = TAG) { "TOTAL BYTES TO INSTALL :${total.toFloat() / (1024 * 1024)}" }
                        showTotalBytes = true
                    }
                    onProgress(readBytes.toFloat() / total)
                }
            }
        }
        statement.execute { response ->
            Logger.d(tag = TAG) { "HTTP RESPONSE CODE: ${response.status}" }
            if (response.status.value != 200)
                return@execute Result.failure(ModelDownloadFailedException())

            // Stream in the source and save it to a cache file
            val channel = response.bodyAsChannel()
            tempFileManage.saveToCache(channel, model.id.toString())
        }
    } catch (e: CancellationException) {
        Logger.w(tag = TAG) { "CANCELLATION OCCURRED WHILE DOWNLOAD" }
        throw e
    } catch (e: Exception) {
        Logger.e(tag = TAG, throwable = e) { "FAILED TO DOWNLOAD THE THING" }
        Result.failure(e)
    }

}
