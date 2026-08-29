package com.sam.talkdraft.model_downloader.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.model_downloader.domain.IDownloadTempFileManager
import com.sam.talkdraft.model_downloader.domain.IModelDownloadManager
import com.sam.talkdraft.model_downloader.domain.IModelDownloadVerifier
import com.sam.talkdraft.model_downloader.domain.IModelFileManager
import com.sam.talkdraft.model_downloader.domain.exceptions.ModelDownloadFailedException
import com.sam.talkdraft.model_downloader.domain.exceptions.ModelVerificationFailedException
import com.sam.talkdraft.model_downloader.domain.models.ModelDownloadStatus
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

private const val TAG = "ModelDownloadManager"

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
        onDownloadState: (ModelDownloadStatus) -> Unit,
    ): Result<Boolean> {

        val modelResult = repository.readModel(modelId)
        if (modelResult.isFailure) {
            val ex = modelResult.exceptionOrNull() ?: Exception("Invalid result")
            return Result.failure(ex)
        }

        val readModel = modelResult.getOrThrow()
        val downloadURI =
            "${readModel.source}/${readModel.repository}/resolve/${readModel.revision}/${readModel.artifactPath}"

        onDownloadState(ModelDownloadStatus.DownloadInitiated)

        // 1. Download the file into temp cache
        var prevProgress = -1
        val downloadResult = downloadModelFile(readModel, downloadURI) { progress ->
            val progressPercentage = (progress * 100).toInt()
            if (progressPercentage > prevProgress) {
                prevProgress = progressPercentage
                Logger.d(tag = TAG) { "DOWNLOAD PROGRESS: $progressPercentage%" }
                onDownloadState(ModelDownloadStatus.Downloading(prevProgress))
            }
        }

        // Handle download failures
        if (downloadResult.isFailure) {
            val exc = downloadResult.exceptionOrNull() as? Exception ?: Exception("Download failed")
            Logger.d(tag = TAG) { "FAILED TO DOWNLOAD THE MODEL" }
            onDownloadState(ModelDownloadStatus.Failed)
            return Result.failure(exc)
        }

        val tempFilePath = downloadResult.getOrThrow()

        // 2. Verify hash
        Logger.d(tag = TAG) { "VERIFYING DOWNLOADED MODEL" }
        onDownloadState(ModelDownloadStatus.Verifying)

        val isVerified = verifier.validateModelHash(tempFilePath, readModel)
        if (!isVerified) {
            Logger.e(tag = TAG) { "MODEL HASH VERIFICATION FAILED" }
            withContext(NonCancellable) {
                tempFileManage.clearCache(tempFilePath)
            }
            onDownloadState(ModelDownloadStatus.Failed)
            return Result.failure(ModelVerificationFailedException())
        }

        // 3. Save model from temp location to permanent storage
        val saveResult = modelFileManager.saveModel(readModel, tempFilePath)

        return saveResult.fold(
            onSuccess = {
                onDownloadState(ModelDownloadStatus.Success)
                Result.success(true)
            },
            onFailure = { error ->
                Logger.e(tag = TAG, throwable = error) { "FAILED TO SAVE MODEL FILE" }
                withContext(NonCancellable) {
                    tempFileManage.clearCache(tempFilePath)
                }
                onDownloadState(ModelDownloadStatus.Failed)
                Result.failure(error)
            },
        )
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
                if (total > 0) {
                    val progress = readBytes.toFloat() / total
                    onProgress(progress)
                }
            }
        }

        statement.execute { response ->
            if (response.status.value != 200) {
                return@execute Result.failure(ModelDownloadFailedException())
            }

            Logger.d(tag = TAG) { "HTTP RESPONSE CODE: ${response.status}" }

            // Stream response body to Okio source
            val channel = response.bodyAsChannel()
            val source = channel.readBuffer().asOkioSource()

            tempFileManage.saveToCache(source, model.id.toString())
        }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }
}
