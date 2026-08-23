package com.sam.talkdraft.model_downloader.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.model_downloader.domain.IDownloadTempFileManager
import com.sam.talkdraft.model_downloader.domain.IModelDownloadManager
import com.sam.talkdraft.model_downloader.domain.IModelDownloadVerifier
import com.sam.talkdraft.model_downloader.domain.IModelFileManager
import com.sam.talkdraft.model_downloader.domain.exceptions.ModelDownloadFailedException
import com.sam.talkdraft.model_downloader.domain.models.ModelDownloadStatus
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import com.sam.talkdraft.model_manager.domain.repository.ITranscriptionModelsRepo
import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.InternalAPI
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
        onDownloadState: (ModelDownloadStatus) -> Unit,
    ): Result<Boolean> {

        val modelResult = repository.readModel(modelId)
        if (modelResult.isFailure) {
            val ex = modelResult.exceptionOrNull() ?: Exception("Invalid result")
            return Result.failure(ex)
        }

        val readModel = modelResult.getOrThrow()
        val downloadURI = "${readModel.source}/${readModel.repository}/${readModel.revision}/${readModel.artifactPath}"

        onDownloadState(ModelDownloadStatus.DownloadInitiated)

        // download the file
        var prevProgress = -1
        val cachedPath = downloadModelFile(readModel, downloadURI) { progress ->
            val progressPercentage = (progress * 100).toInt()
            if (progressPercentage > prevProgress) {
                prevProgress = progressPercentage
                Logger.d(tag = TAG) { "DOWNLOAD PROGRESS :$progressPercentage" }
                onDownloadState(ModelDownloadStatus.Downloading(prevProgress))
            }
        }
        // failed to download the file or failed to save the file
        if (cachedPath.isFailure) {
            val exc = cachedPath.exceptionOrNull() as? Exception ?: Exception("Some exception")
            Logger.d(tag = TAG) { "FAILED TO DOWNLOAD THE MODEL" }
            onDownloadState(ModelDownloadStatus.Failed)
            return Result.failure(exc)
        }

        // verify the hash
        Logger.d(tag = TAG) { "VERIFYING DOWNLOADED MODEL" }
        onDownloadState(ModelDownloadStatus.Verifying)

        val isVerified = verifier.validateModelHash(cachedPath.getOrThrow(), readModel)
        if (!isVerified) {
            withContext(NonCancellable) {
                tempFileManage.clearCache(cachedPath.getOrThrow())
            }
        }

        modelFileManager.saveModel(readModel, cachedPath.getOrThrow()).fold(
            onSuccess = { onDownloadState(ModelDownloadStatus.Success) },
            onFailure = { onDownloadState(ModelDownloadStatus.Failed) },
        )

        return Result.success(true)
    }


    @OptIn(InternalAPI::class)
    private suspend fun downloadModelFile(
        model: TranscriptionModel,
        downloadURL: String,
        onProgress: suspend (Float) -> Unit = {},
    ): Result<Path> = try {
        val statement = httpclient.prepareGet(downloadURL) {
            onDownload { readBytes, totalBytes ->
                val total = totalBytes ?: 0
                if (total > 0) {
                    val progress = readBytes.toFloat() / total
                    onProgress(progress)
                }
            }
        }
        val response = statement.execute()
        if (response.status.value != 200)
            return Result.failure(ModelDownloadFailedException())

        Logger.d(tag = TAG) { "FOUND SOME RESPONSE_CODE: ${response.status}" }
        val channel = response.bodyAsChannel()
        val source = channel.readBuffer.asOkioSource()
        tempFileManage.saveToCache(source, model.id.toString())
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        Result.failure(e)
    }
}
