package com.sam.talkdraft.transcription_android.utils

import java.io.BufferedInputStream
import java.io.File

internal fun File.wavFileToShortArray(): ShortArray {
    val headerSize = 44L
    val pcmByteCount = (length() - headerSize).coerceAtLeast(0L).toInt()
    val shortArray = ShortArray(pcmByteCount / 2)

    BufferedInputStream(inputStream()).use { stream ->
        stream.skip(headerSize)
        val buffer = ByteArray(8192)
        var shortIndex = 0
        while (shortIndex < shortArray.size) {
            val bytesToRead = minOf(buffer.size, (shortArray.size - shortIndex) * 2)
            val bytesRead = stream.read(buffer, 0, bytesToRead)
            if (bytesRead <= 0) break

            for (i in 0 until bytesRead step 2) {
                val low = buffer[i].toInt() and 0xFF
                val high = buffer[i + 1].toInt()
                shortArray[shortIndex++] = ((high shl 8) or low).toShort()
            }
        }
    }
    return shortArray
}

internal inline fun <T> sampleReader(
    initialState: T,
    fullContentArray: ShortArray,
    totalSize: Int,
    chunkSize: Int = 16_000,
    processSample: (ShortArray) -> T,
): T {
    var offset = 0
    var lastState: T = initialState

    while (offset < totalSize) {
        val end = minOf(offset + chunkSize, totalSize)
        val frame = fullContentArray.copyOfRange(offset, end)
        lastState = processSample(frame)
        offset += chunkSize
    }
    return lastState
}
