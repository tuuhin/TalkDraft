package com.sam.talkdraft.transcription_android.vad

internal class JniVadSegment(
    val startSample: Double,
    val endSample: Double,
    val samples: FloatArray,
)
