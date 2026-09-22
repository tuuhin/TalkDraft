package com.sam.talkdraft.ios_archive

import co.touchlab.kermit.Logger
import com.sam.talkdraft.file_archive.exception.IosBZip2CompressionException
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.refTo
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import okio.Path
import platform.darwin.BZ2_bzRead
import platform.darwin.BZ2_bzReadClose
import platform.darwin.BZ2_bzReadOpen
import platform.darwin.BZ2_bzWrite
import platform.darwin.BZ2_bzWriteClose
import platform.darwin.BZ2_bzWriteOpen
import platform.darwin.BZ_OK
import platform.darwin.BZ_STREAM_END
import platform.posix.fclose
import platform.posix.ferror
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fwrite

@OptIn(ExperimentalForeignApi::class)
class NativeBZip2Compressor {

    fun compress(srcPath: Path, destPath: Path) = memScoped {
        val inputFile = fopen(srcPath.toString(), "rb") ?: throw IllegalArgumentException("Missing file path")
        val outFile = fopen(destPath.toString(), "wb") ?: throw IllegalStateException("Outfile failed to create")

        val error = alloc<IntVar>()
        // open the write for compressed write
        val bzFile = BZ2_bzWriteOpen(error.ptr, outFile, 9, 0, 30)
        if (error.value != BZ_OK || bzFile == null)
            throw IosBZip2CompressionException("Unable to initialize bzip2 writer: ${error.value}")

        val buffer = ByteArray(BUFFER_SIZE)
        try {
            // read until read buffer is not zero
            while (true) {
                val readIn = fread(
                    buffer.refTo(0),
                    1u,
                    BUFFER_SIZE.toULong(),
                    inputFile,
                ).toInt()

                // read buffer is zero
                if (readIn == 0) {
                    // issue with the file itself
                    if (ferror(inputFile) != 0) throw IosBZip2CompressionException("Error reading source file")
                    break
                }

                buffer.usePinned { pinned ->
                    BZ2_bzWrite(error.ptr, bzFile, pinned.addressOf(0), readIn)
                }

                if (error.value != BZ_OK) error("bzip2 write failed: ${error.value}")
            }

            // close the write stream
            BZ2_bzWriteClose(error.ptr, bzFile, 0, null, null)

            if (error.value != BZ_OK) throw IosBZip2CompressionException("bzip2 close failed: ${error.value}")
        } catch (e: Exception) {
            Logger.w(tag = TAG, throwable = e) { "FAILED TO PERFORM COMPRESS " }
            // close with abandon
            BZ2_bzWriteClose(error.ptr, bzFile, 1, null, null)
            throw e
        } finally {
            fclose(inputFile)
            fclose(outFile)
        }
    }

    fun decompress(srcPath: Path, destPath: Path) = memScoped {
        val inputFile = fopen(srcPath.toString(), "rb") ?: throw IllegalArgumentException("Missing file path")
        val outFile = fopen(destPath.toString(), "wb") ?: throw IllegalStateException("Outfile failed to create")

        val error = alloc<IntVar>()

        val bzFile = BZ2_bzReadOpen(error.ptr, inputFile, 0, 0, null, 0)

        if (error.value != BZ_OK || bzFile == null)
            throw IosBZip2CompressionException("Unable to initialize bzip2 writer: ${error.value}")

        val buffer = ByteArray(BUFFER_SIZE)
        try {
            // read until read buffer is not zero
            while (true) {
                val readBytes = buffer.usePinned { pinned ->
                    BZ2_bzRead(error.ptr, bzFile, pinned.addressOf(0), buffer.size)
                }

                // has some readable bytes
                if (readBytes > 0) fwrite(buffer.refTo(0), 1u, readBytes.toULong(), outFile)

                when (error.value) {
                    BZ_OK -> continue
                    BZ_STREAM_END -> break
                    else -> throw IosBZip2CompressionException("bzip2 read failed: ${error.value}")
                }
            }

            // close the read stream
            BZ2_bzReadClose(error.ptr, bzFile)

            if (error.value != BZ_OK) throw IosBZip2CompressionException("bzip2 close failed: ${error.value}")
        } catch (e: Exception) {
            Logger.w(tag = TAG, throwable = e) { "FAILED TO PERFORM DECOMPRESS" }
            BZ2_bzWriteClose(error.ptr, bzFile, 1, null, null)
            throw e
        } finally {
            fclose(inputFile)
            fclose(outFile)
        }
    }

    companion object {
        private const val TAG = "BZIP2_COMPRESSOR"
        private const val BUFFER_SIZE = 64 * 1024
    }

}
