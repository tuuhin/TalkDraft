package com.sam.talkdraft.transcription.data.models

import okio.Path

internal data class ZipFormerModelPath(
    val encoderPath: String,
    val decoderPath: String,
    val joinerPath: String,
    val tokenPath: String,
) {
    internal constructor(
        encoderPath: Path,
        decoderPath: Path,
        joinerPath: Path,
        tokenPath: Path,
    ) : this(encoderPath.toString(), decoderPath.toString(), joinerPath.toString(), tokenPath.toString())
}
