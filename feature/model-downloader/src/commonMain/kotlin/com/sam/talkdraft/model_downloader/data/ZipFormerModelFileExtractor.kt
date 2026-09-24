package com.sam.talkdraft.model_downloader.data

import co.touchlab.kermit.Logger
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.common.platform.IPlatformFilePathProvider
import com.sam.talkdraft.file_archive.domain.IArchiveExtractor
import com.sam.talkdraft.file_archive.domain.models.ArchiveFormats
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.SYSTEM
import org.koin.core.annotation.Factory

@Factory
internal class ZipFormerModelFileExtractor(
    private val archiveExtractor: IArchiveExtractor,
    private val fileProvider: IPlatformFilePathProvider,
    private val dispatchers: IPlatformCoroutineDispatchers,
) {
    private val interMediateDir by lazy { fileProvider.providesCachesDirPath() / "intermediate" }
    private val fs = FileSystem.SYSTEM

    @OptIn(ExperimentalStdlibApi::class)
    suspend fun extractAndSetModelFiles(archivePath: Path, finalPath: Path) = runCatching {
        try {
            // extract the path to an intermediate path
            archiveExtractor.extract(
                src = archivePath,
                dest = interMediateDir,
                formats = ArchiveFormats.TAR_BZIP2,
            ).getOrThrow()

            withContext(dispatchers.io) {
                val allFiles = fs.listRecursively(interMediateDir).toList()
                // take the encoder ,decoder and joiner
                val modelFiles = allFiles.filter { path ->
                    val name = path.name
                    name.endsWith(".int8.onnx") && (
                        name.startsWith("encoder") ||
                            name.startsWith("decoder") ||
                            name.startsWith("joiner")
                        )
                }

                Logger.d(tag = TAG) { "ZIP FORMER FILES FOUND :$modelFiles" }

                if (modelFiles.size < 3) throw Exception("Missing some required file")
                val tokenFiles = allFiles.firstOrNull { it.name == "tokens.txt" }
                    ?: throw Exception("Missing token file")

                // Combine all targeted files
                val filesToCopy = modelFiles + tokenFiles

                if (!fs.exists(finalPath)) fs.createDirectories(finalPath)

                supervisorScope {
                    val operations = filesToCopy.map { file ->
                        async(dispatchers.io) {
                            // format the file names
                            val (name, ext) = file.nameAndExtension
                            val extension = ext ?: ".onnx"
                            val finalFileName = when {
                                name.startsWith("encoder") -> "encoder.$extension"
                                name.startsWith("decoder") -> "decoder.$extension"
                                name.startsWith("joiner") -> "joiner.$extension"
                                else -> file.name
                            }

                            val destinationFile = finalPath / finalFileName
                            // copy the files
                            fs.copy(file, destinationFile)
                            Logger.d(tag = TAG) { "Successfully copied: ${file.name} to $destinationFile" }
                        }
                    }
                    operations.awaitAll()
                }
            }
        } finally {
            withContext(dispatchers.io + NonCancellable) {
                if (fs.exists(interMediateDir)) {
                    Logger.d(tag = TAG) { "DELETING THE INTERMEDIATE DIRECTORY" }
                    fs.deleteRecursively(interMediateDir)
                }
            }
        }
    }


    private val Path.nameAndExtension: Pair<String, String?>
        get() {
            val fileName = this.name
            val dotIndex = fileName.lastIndexOf('.')
            val justFileName = fileName.substring(0, dotIndex)
            val extension = fileName.substring(dotIndex)
            return if (dotIndex == -1) fileName to null
            else justFileName to extension
        }

    companion object {
        private const val TAG = "ZipFormerModelFileExtractor"
    }
}
