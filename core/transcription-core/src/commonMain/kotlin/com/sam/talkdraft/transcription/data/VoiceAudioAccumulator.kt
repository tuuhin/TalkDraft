package com.sam.talkdraft.transcription.data

import com.sam.talkdraft.transcription.domain.model.VoiceDetectionResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Factory

internal typealias SpeechSegment = ShortArray

@Factory
internal class VoiceAudioAccumulator(
    private val speechThreshold: Float = 0.7f,
    private val silenceThreshold: Float = 0.2f,
) {
    private val lock = Mutex()

    private var isSpeaking = false
    private val buffer = mutableListOf<ShortArray>()

    suspend fun process(audioFrame: ShortArray, detection: VoiceDetectionResult): SpeechSegment? = lock.withLock {
        val probability = detection.probability

        when {
            !isSpeaking && probability >= speechThreshold -> {
                isSpeaking = true
                buffer += audioFrame.copyOf()
                null
            }

            isSpeaking && probability <= silenceThreshold -> {
                isSpeaking = false
                buffer += audioFrame.copyOf()
                createSegmentLocked()
            }

            isSpeaking -> {
                buffer += audioFrame.copyOf()
                null
            }

            else -> {
                null
            }
        }
    }

    suspend fun flush(): SpeechSegment? = lock.withLock {
        if (!isSpeaking || buffer.isEmpty()) {
            return@withLock null
        }

        isSpeaking = false
        createSegmentLocked()
    }

    suspend fun reset() = lock.withLock {
        isSpeaking = false
        buffer.clear()
    }

    private fun createSegmentLocked(): SpeechSegment {
        val size = buffer.sumOf { it.size }
        val audio = ShortArray(size)

        var offset = 0
        for (frame in buffer) {
            frame.copyInto(
                destination = audio,
                destinationOffset = offset,
            )
            offset += frame.size
        }

        buffer.clear()
        return audio
    }
}
