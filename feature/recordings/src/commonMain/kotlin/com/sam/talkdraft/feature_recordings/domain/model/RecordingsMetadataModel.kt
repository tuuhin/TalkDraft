package com.sam.talkdraft.feature_recordings.domain.model

import kotlin.time.Duration
import kotlinx.datetime.LocalDateTime

data class RecordingsMetadataModel(
    val title: String,
    val audioPath: String,
    val totalDuration: Duration,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val isPinned: Boolean = false,
    val isFavourite: Boolean = false,
    val language: String = "en",
)
