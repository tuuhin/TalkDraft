package com.sam.talkdraft.model_downloader.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.model_downloader.domain.IDownloadTempFileManager
import com.sam.talkdraft.model_downloader.domain.IModelDownloadManager
import com.sam.talkdraft.model_downloader.domain.IModelDownloadVerifier
import com.sam.talkdraft.model_downloader.domain.IModelFileManager
import com.sam.talkdraft.model_downloader.domain.exceptions.ModelDownloadFailedException
import com.sam.talkdraft.model_downloader.domain.exceptions.ModelVerificationFailedException
import com.sam.talkdraft.model_downloader.domain.models.ModelDownloadStatus
import com.sam.talkdraft.model_manager.domain.model.LocalModelStatus
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import com.sam.talkdraft.model_manager.domain.repository.ITranscriptionModelsRepo
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.readBuffer
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.io.okio.asOkioSource
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
        val downloadURI = transcriptionModel.getToURL()
        // update model state to downloading
        repository.updateModelStatus(transcriptionModel.id, LocalModelStatus.DOWNLOADING)
        onDownloadState(ModelDownloadStatus.DownloadInitiated)

        var tempFilePath: Path? = null

        return try {
            // 1. Download the file into temp cache
            var prevProgress = -1
            val downloadResult = downloadModelFile(transcriptionModel, downloadURI) { progress ->
                val progressPercentage = (progress * 100).toInt()
                if (progressPercentage > prevProgress) {
                    prevProgress = progressPercentage
                    Logger.d(tag = TAG) { "DOWNLOAD PROGRESS: $progressPercentage%" }
                    onDownloadState(ModelDownloadStatus.Downloading(prevProgress.toFloat()))
                }
            }

            // Handle download failures
            if (downloadResult.isFailure) {
                val exc = downloadResult.exceptionOrNull() as? Exception ?: Exception("Download failed")
                Logger.d(tag = TAG) { "FAILED TO DOWNLOAD THE MODEL" }
                withContext(NonCancellable) {
                    // update model state to not installed as it's a failed download
                    repository.updateModelStatus(transcriptionModel.id, LocalModelStatus.NOT_INSTALLED)
                }
                onDownloadState(ModelDownloadStatus.Failed(downloadResult.exceptionOrNull()?.message))
                return Result.failure(exc)
            }

            tempFilePath = downloadResult.getOrThrow()
            // 2. Verify hash
            Logger.d(tag = TAG) { "VERIFYING DOWNLOADED MODEL" }
            onDownloadState(ModelDownloadStatus.Verifying)

            val isVerified = verifier.validateModelHash(tempFilePath, transcriptionModel)
            if (!isVerified) {
                Logger.e(tag = TAG) { "MODEL HASH VERIFICATION FAILED" }
                withContext(NonCancellable) {
                    // update model state to not installed as it's a failed download
                    repository.updateModelStatus(transcriptionModel.id, LocalModelStatus.NOT_INSTALLED)
                    // delete the temporary downloaded model
                    tempFileManage.clearCache(tempFilePath)
                }
                onDownloadState(ModelDownloadStatus.Failed("Verification failed"))
                return Result.failure(ModelVerificationFailedException())
            }

            // 3. Save model from temp location to permanent storage
            val saveResult = modelFileManager.saveModel(transcriptionModel, tempFilePath)

            saveResult.fold(
                onSuccess = {
                    onDownloadState(ModelDownloadStatus.Success)
                    Result.success(true)
                },
                onFailure = { error ->
                    Logger.e(tag = TAG, throwable = error) { "FAILED TO SAVE MODEL FILE" }
                    withContext(NonCancellable) {
                        tempFileManage.clearCache(tempFilePath)
                    }
                    onDownloadState(ModelDownloadStatus.Failed(error.message))
                    Result.failure(error)
                },
            )

        } catch (e: CancellationException) {
            Logger.w(tag = TAG) { "OPERATION CANCELLED" }
            onDownloadState(ModelDownloadStatus.Failed("Operation Cancelled"))
            throw e
        } catch (e: Exception) {
            Logger.e(tag = TAG, throwable = e) { "FAILED TO PERFORM DOWNLOAD" }
            Result.failure(e)
        } finally {
            withContext(NonCancellable) {
                val currentModel = repository.readModel(modelId).getOrNull()
                if (currentModel?.modelStatus != LocalModelStatus.INSTALLED) {
                    Logger.d(tag = TAG) { "RESETTING THE MODEL AS STATUS IS NOT INSTALLED" }
                    repository.updateModelStatus(transcriptionModel.id, LocalModelStatus.NOT_INSTALLED)
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
        val statement = httpclient.prepareGet(downloadURL) {
            url {
                parameters.append("download", "true")
            }
            onDownload { readBytes, totalBytes ->
                val total = totalBytes ?: 0
                if (total > 0) onProgress(readBytes.toFloat() / total)
            }
        }
        statement.execute { response ->
            Logger.d(tag = TAG) { "HTTP RESPONSE CODE: ${response.status}" }
            if (response.status.value != 200)
                return@execute Result.failure(ModelDownloadFailedException())

            // Stream in the source and save it to a cache file
            val channel = response.bodyAsChannel()
            val source = channel.readBuffer().asOkioSource()
            tempFileManage.saveToCache(source, model.id.toString())
        }
    } catch (e: CancellationException) {
        Logger.w(tag = TAG) { "CANCELLATION OCCURRED WHILE DOWNLOAD" }
        throw e
    } catch (e: Exception) {
        Logger.e(tag = TAG, throwable = e) { "FAILED TO DOWNLOAD THE THING" }
        Result.failure(e)
    }

    private fun TranscriptionModel.getToURL(): String =
        "${source}/${repository}/resolve/${revision}/${artifactPath}"
}
