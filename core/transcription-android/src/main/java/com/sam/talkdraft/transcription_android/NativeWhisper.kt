package com.sam.talkdraft.transcription_android

import android.util.Log
import com.sam.talkdraft.transcription_android.models.ProcessingState
import com.sam.talkdraft.transcription_android.models.WhisperErrorCode
import com.sam.talkdraft.transcription_android.models.WhisperState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi


@OptIn(ExperimentalAtomicApi::class)
class NativeWhisper : AutoCloseable {

    private val _nativeHandle = AtomicLong(0L)
    private val _isInitialized = AtomicBoolean(false)
    private val lock = Any()

    private val audioBuffer: ByteBuffer = ByteBuffer
        .allocateDirect(MAX_AUDIO_SAMPLES * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())

    private val _floatBuffer = audioBuffer.asFloatBuffer()
    private var accumulatedSampleCount = 0

    fun initialize(modelPath: String, language: String = "auto", useGpu: Boolean = true): Boolean = synchronized(lock) {
        if (_isInitialized.load()) throw IllegalStateException("A instance of whisper is already running")

        val handle = initializeNative(modelPath, language, useGpu)
        if (handle == 0L) return false

        if (!_isInitialized.compareAndSet(expectedValue = false, newValue = true)) {
            destroyNative(handle)
            throw IllegalStateException("NativeWhisper was initialized on another thread.")
        }
        _nativeHandle.store(handle)
        accumulatedSampleCount = 0
        return true
    }

    fun processSamples(audioFrame: ShortArray): ProcessingState = synchronized(lock) {
        if (!_isInitialized.load()) throw IllegalStateException("Whisper is not instantiated")
        if (audioFrame.isEmpty()) return ProcessingState.Buffering

        val handle = _nativeHandle.load()
        check(handle != 0L) { "Whisper native handle is invalid." }

        val spaceAvailable = MAX_AUDIO_SAMPLES - accumulatedSampleCount
        if (spaceAvailable <= 0) {
            Log.w(TAG, "Utterance buffer full at $MAX_AUDIO_SAMPLES samples (30s). Call reset().")
            return ProcessingState.Error(WhisperErrorCode.BUFFER_FULL)
        }

        val samplesToCopy = minOf(spaceAvailable, audioFrame.size)
        _floatBuffer.position(accumulatedSampleCount)
        for (i in 0 until samplesToCopy) {
            _floatBuffer.put(audioFrame[i] * FLOAT_MULTIPLIER)
        }
        accumulatedSampleCount += samplesToCopy

        // need to process a few more frames to continue
        if (accumulatedSampleCount < MIN_INFERENCE_SAMPLES) return ProcessingState.Buffering

        audioBuffer.position(0)
        audioBuffer.limit(accumulatedSampleCount * Float.SIZE_BYTES)
        val nativeSuccess = processNativeDirectBuffer(handle, audioBuffer, accumulatedSampleCount)
        return if (nativeSuccess) ProcessingState.Success else ProcessingState.Error(readError())
    }

    fun reset() = synchronized(lock) {
        accumulatedSampleCount = 0
    }

    fun readState(): WhisperState? = synchronized(lock) {
        if (!_isInitialized.load()) return null
        return readStateNative(_nativeHandle.load())
    }

    fun readError(): WhisperErrorCode? = synchronized(lock) {
        if (!_isInitialized.load()) return null
        val code = readErrorNative(_nativeHandle.load())
        return WhisperErrorCode.fromCode(code)
    }

    override fun close(): Unit = synchronized(lock) {
        if (_isInitialized.compareAndSet(expectedValue = true, newValue = false)) {
            val handle = _nativeHandle.exchange(0L)
            if (handle != 0L) destroyNative(handle)
            accumulatedSampleCount = 0
        } else {
            Log.w(TAG, "Close ignored: NativeWhisper was not initialized or already closed.")
        }
    }

    private external fun initializeNative(modelPath: String, language: String, useGpu: Boolean): Long
    private external fun processNativeDirectBuffer(handle: Long, buffer: ByteBuffer, length: Int): Boolean
    private external fun readStateNative(handle: Long): WhisperState?
    private external fun readErrorNative(handle: Long): Int
    private external fun destroyNative(handle: Long)

    companion object {
        private const val TAG = "NativeWhisper"

        private const val FLOAT_MULTIPLIER = 1.0f / 32768.0f
        private const val MAX_AUDIO_SAMPLES = 16000 * 30
        private const val MIN_INFERENCE_SAMPLES = 16_000

        init {
            System.loadLibrary("native_transcriptions")
        }
    }
}
