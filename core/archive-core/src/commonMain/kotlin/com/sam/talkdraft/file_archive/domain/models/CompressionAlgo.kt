package com.sam.talkdraft.file_archive.domain.models

internal sealed class CompressionAlgo {
    data object GZip : CompressionAlgo()
    data object BZip2 : CompressionAlgo()
}
