package com.sam.talkdraft.feature_recordings.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.common.platform.IPlatformFilePathProvider
import com.sam.talkdraft.feature_recordings.domain.IRecordingFileManager
import com.sam.talkdraft.feature_recordings.domain.exception.EmptyFileException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import org.koin.core.annotation.Factory

@Factory(binds = [IRecordingFileManager::class])
internal class RecordingFileManagerImpl(
    private val filePathProvider: IPlatformFilePathProvider,
    private val dispatchers: IPlatformCoroutineDispatchers,
) : IRecordingFileManager {

    private val fs = FileSystem.SYSTEM

    private val recordingDir: Path by lazy {
        val path = filePathProvider.providesFileDirPath() / "recording_files"
        if (!fs.exists(path)) fs.createDirectory(path)
        path
    }

    override val readRecordingPath: Path
        get() = recordingDir

    override suspend fun saveFileContent(
        fileName: String,
        contentFilePath: Path,
        deleteContentFile: Boolean,
    ): Result<Path> {
        return withContext(dispatchers.io) {

            if (!fs.exists(contentFilePath)) {
                Logger.w(tag = TAG) { "CONTENT FILE ITSELF IS MISSING" }
                return@withContext Result.failure(EmptyFileException())
            }

            val meta = fs.metadata(contentFilePath)
            if (meta.size == null || meta.size == 0L) {
                Logger.w(tag = TAG) { "CONTENT FILE HAS NO DATA" }
                return@withContext Result.failure(EmptyFileException())
            }

            val newFile = readRecordingPath / fileName
            try {
                // copy the file content
                fs.write(newFile) {
                    fs.read(contentFilePath) {
                        this@write.writeAll(this)
                    }
                }
                // delete the file
                if (deleteContentFile) {
                    Logger.d(tag = TAG) { "DELETING CONTENT FILE" }
                    fs.delete(contentFilePath)
                }
                Result.success(newFile)
            } catch (e: Exception) {
                withContext(NonCancellable) {
                    if (fs.exists(newFile)) fs.delete(newFile)
                }
                if (e is CancellationException) throw e
                Logger.e(tag = TAG, throwable = e) { "FAILED TO SAVE THE RECORDING FILE PATH" }
                Result.failure(e)
            }
        }
    }

    companion object {
        private const val TAG = "RECORDING_FILE_MANAGER"
    }
}
