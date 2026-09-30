package com.sam.talkdraft.transcription.ios.models

data class IosTranscriptionResultState(
    val fullText: String = "",
    val segment: List<IosTranscriptionResultSegment> = emptyList(),
)
