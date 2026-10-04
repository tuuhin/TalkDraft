package com.sam.talkdraft.player

import com.sam.talkdraft.player.model.AudioMetaData
import okio.Path

expect class PlatformAudioMetadataExtractor : IAudioMetadataExtractor {
    override suspend fun extractMetadataFromAudioFilePath(path: Path): Result<AudioMetaData>
}
