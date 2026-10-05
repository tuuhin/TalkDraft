package com.sam.talkdraft.archive_android

import com.sam.talkdraft.archive_android.exceptions.NativeArchiveException
import okio.FileSystem
import okio.Path

class NativeTarArchiver {

    val fs = FileSystem.SYSTEM

    fun createTar(srcDirectory: Path, destPath: Path, onProgress: ((Float) -> Unit)? = null): Result<Unit> {
        return runCatching {

            val isDirectory = fs.metadataOrNull(srcDirectory)?.isDirectory ?: false
            if (!isDirectory) throw IllegalArgumentException("Src path needs to a directory cant align a file")

            // if dest path parent is absent create directories
            destPath.parent?.let { if (!fs.exists(it)) fs.createDirectories(it) }

            val listener = if (onProgress == null) null
            else ArchiveProgressListener { progress -> onProgress(progress) }

            val success = createTar(srcDirectory.toString(), destPath.toString(), listener)
            if (!success) throw NativeArchiveException("Failed to create TAR archive from $srcDirectory")
        }
    }

    fun extractTar(srcPath: Path, destPath: Path, onProgress: ((Float) -> Unit)? = null): Result<Unit> {
        return runCatching {

            if (!fs.exists(srcPath)) throw IllegalArgumentException("Missing src path")
            if (!fs.exists(destPath)) fs.createDirectories(destPath)

            val listener = if (onProgress == null) null
            else ArchiveProgressListener { progress -> onProgress(progress) }

            val success = extractTar(srcPath.toString(), destPath.toString(), listener)
            if (!success) throw NativeArchiveException("Failed to extract TAR archive from $srcPath")
        }
    }

    @Throws(NativeArchiveException::class)
    private external fun createTar(
        inputPath: String,
        outputPath: String,
        progressListener: ArchiveProgressListener?,
    ): Boolean

    @Throws(NativeArchiveException::class)
    private external fun extractTar(
        inputPath: String,
        outputPath: String,
        progressListener: ArchiveProgressListener?,
    ): Boolean

    companion object {
        init {
            System.loadLibrary("android_archiver")
        }
    }
}
