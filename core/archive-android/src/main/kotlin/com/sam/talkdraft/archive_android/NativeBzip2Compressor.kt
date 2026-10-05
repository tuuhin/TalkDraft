package com.sam.talkdraft.archive_android

import com.sam.talkdraft.archive_android.exceptions.NativeCompressionException
import okio.Path


class NativeBzip2Compressor {

    fun compress(srcPath: Path, destPath: Path, onProgress: ((Float) -> Unit)? = null) {

        val listener = if (onProgress == null) null
        else ArchiveProgressListener { progress -> onProgress(progress) }

        compressBzip2(
            inputPath = srcPath.toFile().absolutePath,
            outputPath = destPath.toString(),
            progressListener = listener,
        )
    }

    fun decompress(srcPath: Path, destPath: Path, onProgress: ((Float) -> Unit)? = null) {
        val listener = if (onProgress == null) null
        else ArchiveProgressListener { progress -> onProgress(progress) }

        decompressBzip2(
            inputPath = srcPath.toFile().absolutePath,
            outputPath = destPath.toString(),
            progressListener = listener,
        )
    }

    @Throws(NativeCompressionException::class)
    private external fun compressBzip2(
        inputPath: String,
        outputPath: String,
        progressListener: ArchiveProgressListener?,
    ): Boolean

    @Throws(NativeCompressionException::class)
    private external fun decompressBzip2(
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
