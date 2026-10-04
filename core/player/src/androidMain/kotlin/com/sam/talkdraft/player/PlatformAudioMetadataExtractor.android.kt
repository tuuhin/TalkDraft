package com.sam.talkdraft.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.concurrent.futures.await
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.inspector.MetadataRetriever
import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.player.exception.InvalidAudioFilePathException
import com.sam.talkdraft.player.model.AudioMetaData
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import org.koin.core.annotation.Factory

@Factory(binds = [IAudioMetadataExtractor::class])
actual class PlatformAudioMetadataExtractor(
    private val dispatchers: IPlatformCoroutineDispatchers,
    private val context: Context,
) : IAudioMetadataExtractor {

    private val fs = FileSystem.SYSTEM

    @OptIn(UnstableApi::class)
    actual override suspend fun extractMetadataFromAudioFilePath(path: Path): Result<AudioMetaData> {
        return runCatching {
            val isPresent = withContext(dispatchers.io) {
                fs.exists(path)
            }
            if (!isPresent) throw InvalidAudioFilePathException()

            val mediaItem = MediaItem.fromUri(path.toFile().toUri())
            val retriever = MetadataRetriever.Builder(context, mediaItem)
                .build()

            retriever.use {
                val durationUs = withContext(dispatchers.main) { it.retrieveDurationUs().await() }
                    .toDuration(DurationUnit.MICROSECONDS)
                AudioMetaData(duration = durationUs)
            }
        }.onFailure { err -> if (err is CancellationException) throw err }
    }
}
