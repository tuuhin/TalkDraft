package com.sam.talkdraft.player

import com.sam.talkdraft.player.model.AudioMetaData
import okio.Path

interface IAudioMetadataExtractor {

    suspend fun extractMetadataFromAudioFilePath(path: Path): Result<AudioMetaData>
}
