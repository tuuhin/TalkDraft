package com.sam.talkdraft.transcription.data

import com.sam.talkdraft.transcription.domain.ITranscriptionEngine
import com.sam.talkdraft.transcription.domain.model.TranscriptionRequestMetadata
import com.sam.talkdraft.transcription.domain.model.TranscriptionState

internal actual class PlatformTranscriptionEngine : ITranscriptionEngine {

    actual override fun warmUp(request: TranscriptionRequestMetadata) {
    }

    actual override fun process(bytes: ShortArray): TranscriptionState {
        TODO("Not yet implemented")
    }

    actual override fun cleanUp() {
    }
}
