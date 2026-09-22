package com.sam.talkdraft.archive_android

import com.sam.talkdraft.archive_android.exceptions.NativeArchiveException
import okio.FileSystem
import okio.Path

class NativeTarArchiver {

    private val fs = FileSystem.SYSTEM

    fun createTar(srcDirectory: Path, destPath: Path): Result<Unit> {
        return runCatching {
            val isDirectory = fs.metadataOrNull(srcDirectory)
                ?.isDirectory ?: false

            if (!isDirectory) throw IllegalArgumentException("SRC path need to be an directory")
            val inputFile = srcDirectory.toFile()

            // if dest path parent is absent create directories
            destPath.parent?.let { if (!fs.exists(it)) fs.createDirectories(it) }

            val success = createTar(srcDirectory.toString(), destPath.toString())
            if (!success) throw NativeArchiveException("Failed to create TAR archive from $srcDirectory")
        }
    }

    fun extractTar(srcPath: Path, destPath: Path): Result<Unit> {
        return runCatching {
            if (!fs.exists(srcPath)) throw IllegalArgumentException("Missing src file")

            if (!fs.exists(destPath)) fs.createDirectories(destPath)

            val success = extractTar(srcPath.toString(), destPath.toString())
            if (!success) throw NativeArchiveException("Failed to extract TAR archive from $srcPath")
        }
    }

    @Throws(NativeArchiveException::class)
    private external fun createTar(inputPath: String, outputPath: String): Boolean

    @Throws(NativeArchiveException::class)
    private external fun extractTar(inputPath: String, outputPath: String): Boolean

    companion object {
        init {
            System.loadLibrary("android_archiver")
        }
    }
}
