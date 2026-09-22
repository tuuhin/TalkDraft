package com.sam.talkdraft.archive_android

import com.sam.talkdraft.archive_android.exceptions.NativeCompressionException
import okio.Path

class NativeBzip2Compressor {

    fun compress(srcPath: Path, destPath: Path) {
        compressBzip2(srcPath.toFile().absolutePath, destPath.toString())
    }

    fun decompress(srcPath: Path, destPath: Path) {
        decompressBzip2(srcPath.toFile().absolutePath, destPath.toString())
    }

    @Throws(NativeCompressionException::class)
    private external fun compressBzip2(inputPath: String, outputPath: String): Boolean

    @Throws(NativeCompressionException::class)
    private external fun decompressBzip2(inputPath: String, outputPath: String): Boolean

    companion object {
        init {
            System.loadLibrary("android_archiver")
        }
    }
}
