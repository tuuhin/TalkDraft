package com.sam.talkdraft.model_downloader.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.common.platform.IPlatformFilePathProvider
import com.sam.talkdraft.model_downloader.domain.IModelFileManager
import com.sam.talkdraft.model_downloader.domain.exceptions.ModelFileAlreadyExistsException
import com.sam.talkdraft.model_manager.domain.model.ModelInstallStatus
import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import com.sam.talkdraft.model_manager.domain.repository.ITranscriptionModelsRepo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.koin.core.annotation.Factory

private const val TAG = "ModelDownloadFileManager"

@Factory(binds = [IModelFileManager::class])
internal class ModelDownloadFileManager(
    private val repo: ITranscriptionModelsRepo,
    private val fileProvider: IPlatformFilePathProvider,
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IModelFileManager {

    private val fs = FileSystem.SYSTEM
    private val readModelPath by lazy { fileProvider.providesFileDirPath() / "transcription_models" }

    override suspend fun deleteModelFile(model: TranscriptionModel): Result<Boolean> {
        return runCatching {

            val path = model.modelPath?.toPath() ?: (readModelPath / model.id.toHexString())

            withContext(dispatchers.io) {
                if (fs.exists(path)) {
                    withContext(NonCancellable) {
                        fs.delete(path)
                        Logger.d(tag = TAG) { "FILE DELETED SUCCESSFULLY $path" }
                    }
                }
                // Clean up empty parent directory if left over
                val parentDir = path.parent
                if (parentDir != null && fs.exists(parentDir) && fs.list(parentDir).isEmpty()) {
                    withContext(NonCancellable) {
                        fs.delete(parentDir)
                        Logger.d(tag = TAG) { "CLEAN UP THE PARENT DIRECTORY $path" }
                    }
                }
                repo.updateModelPath(model.id, null)
                repo.updateModelStatus(model.id, ModelInstallStatus.NOT_INSTALLED)
            }
            true
        }.onFailure { e ->
            if (e is CancellationException) throw e
            Logger.e(tag = TAG, throwable = e) { "FAILED TO DELETE THE FILE" }
        }
    }

    override suspend fun saveModel(model: TranscriptionModel, cachedPath: Path, overwrite: Boolean): Result<Unit> {
        return runCatching {
            val oldPath = model.modelPath?.toPath()
            val newModelPath = readModelPath / model.id.toHexString()

            withContext(dispatchers.io) {
                if (!fs.exists(cachedPath))
                    throw IllegalStateException("Source cached file does not exist at: $cachedPath")

                if (oldPath != null && fs.exists(oldPath) && !overwrite) throw ModelFileAlreadyExistsException()


                if (fs.exists(newModelPath)) fs.delete(newModelPath)

                newModelPath.parent?.let { parentDir ->
                    fs.createDirectories(parentDir)
                }
                try {
                    fs.copy(cachedPath, newModelPath)
                    repo.updateModelStatus(modelId = model.id, status = ModelInstallStatus.INSTALLED)
                    repo.updateModelPath(modelId = model.id, path = newModelPath.toString())
                } catch (e: CancellationException) {
                    Logger.d(tag = TAG) { "Operation cancelled, deleting partial file" }
                    withContext(NonCancellable) {
                        if (fs.exists(newModelPath)) fs.delete(newModelPath)
                    }
                    throw e
                }
            }
            Logger.d(tag = TAG) { "File copied successfully to $newModelPath" }
        }.onFailure { e ->
            if (e is CancellationException) throw e
            Logger.e(tag = TAG, throwable = e) { "FAILED TO SAVE THE FILE" }
        }
    }
}
