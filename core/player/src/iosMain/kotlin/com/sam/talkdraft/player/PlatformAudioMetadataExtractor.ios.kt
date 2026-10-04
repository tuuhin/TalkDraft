package com.sam.talkdraft.player

import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.player.exception.InvalidAudioFilePathException
import com.sam.talkdraft.player.model.AudioMetaData
import kotlin.time.Duration.Companion.seconds
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSError
import platform.Foundation.NSURL

actual class PlatformAudioMetadataExtractor(
    private val dispatchers: IPlatformCoroutineDispatchers,
) :
    IAudioMetadataExtractor {
    private val fs = FileSystem.SYSTEM

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual override suspend fun extractMetadataFromAudioFilePath(path: Path): Result<AudioMetaData> {
        return runCatching {
            // check the file path
            val isPresent = withContext(dispatchers.io) { fs.exists(path) }
            if (!isPresent) throw InvalidAudioFilePathException()

            // create a player with the file path
            val duration = memScoped {
                val url = NSURL.fileURLWithPath(path.toString())
                val error = alloc<ObjCObjectVar<NSError?>>()
                val player = AVAudioPlayer(contentsOfURL = url, error = error.ptr)
                if (error.value != null)
                    throw IllegalStateException("Failed to prepare player to read audio duration")
                player.duration
            }
            // use the duration data
            AudioMetaData(duration = duration.seconds)
        }.onFailure { err -> if (err is CancellationException) throw err }
    }
}
