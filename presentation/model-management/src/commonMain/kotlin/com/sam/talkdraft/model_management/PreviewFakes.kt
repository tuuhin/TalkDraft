package com.sam.talkdraft.model_management

import com.sam.talkdraft.model_manager.domain.model.TranscriptionModel
import com.sam.talkdraft.model_manager.domain.model.TranscriptionType
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal object PreviewFakes {

    val FAKE_STREAMING_MODEL = TranscriptionModel(
        id = Uuid.random(),
        displayName = "Compose Model",
        type = TranscriptionType.STREAMING,
        version = "1.0.0",
        cachedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        sizeInBytes = 100 * 1024 * 1024,
        modelPath = "",
    )
}
