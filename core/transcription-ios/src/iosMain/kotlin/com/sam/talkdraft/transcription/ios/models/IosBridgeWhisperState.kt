package com.sam.talkdraft.transcription.ios.models

data class IosBridgeWhisperState(
    val fullText: String = "",
    val segment: List<IosBridgeWhisperSegment> = emptyList(),
)
