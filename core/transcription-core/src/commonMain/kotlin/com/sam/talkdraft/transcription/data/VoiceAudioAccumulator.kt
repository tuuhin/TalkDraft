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
    private val maxSilenceFrames: Int = 20,
) {
    private val lock = Mutex()

    private var isSpeaking = false
    private var silenceFrames = 0
    private val buffer = mutableListOf<ShortArray>()

    suspend fun process(audioFrame: ShortArray, detection: VoiceDetectionResult): SpeechSegment? = lock.withLock {
        if (audioFrame.isEmpty()) return@withLock null

        val probability = detection.probability

        if (!isSpeaking) {
            handleIdleFrame(audioFrame = audioFrame, probability = probability)
            return@withLock null
        }

        handleSpeakingFrame(audioFrame = audioFrame, probability = probability)
    }

    private fun handleIdleFrame(audioFrame: ShortArray, probability: Float) {
        if (probability < speechThreshold) return
        // Speech has started.
        isSpeaking = true
        silenceFrames = 0
        buffer += audioFrame.copyOf()
    }

    private fun handleSpeakingFrame(audioFrame: ShortArray, probability: Float): SpeechSegment? {
        buffer += audioFrame.copyOf()

        if (probability <= silenceThreshold) {
            silenceFrames++
            if (silenceFrames >= maxSilenceFrames) return finishSegment()
        } else silenceFrames = 0
        return null
    }


    suspend fun flush(): SpeechSegment? = lock.withLock {
        if (!isSpeaking || buffer.isEmpty()) return@withLock null
        finishSegment()
    }

    suspend fun reset() = lock.withLock {
        resetLocked()
    }

    private fun finishSegment(): SpeechSegment {
        val segment = createSegmentLocked()
        isSpeaking = false
        silenceFrames = 0
        return segment
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

    private fun resetLocked() {
        isSpeaking = false
        silenceFrames = 0
        buffer.clear()
    }
}
