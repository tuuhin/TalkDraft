package com.sam.talkdraft.transcription_android

import android.util.Log
import com.sam.talkdraft.transcription_android.models.WhisperErrorCode
import com.sam.talkdraft.transcription_android.models.WhisperState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val TAG = "NativeWhisper"

@OptIn(ExperimentalAtomicApi::class)
class NativeWhisper : AutoCloseable {

    private val nativeHandle = AtomicLong(0L)
    private val _isInitialized = AtomicBoolean(false)

    private val audioBuffer: ByteBuffer = ByteBuffer
        .allocateDirect(MAX_AUDIO_SAMPLES * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())

    private val _floatBuffer = audioBuffer.asFloatBuffer()

    /**
     * Initializes the native Whisper context using a file path.
     */
    fun initialize(modelPath: String, language: String = "auto", useGpu: Boolean = true): Boolean {
        val isInit = _isInitialized.load()
        if (isInit) throw IllegalStateException("A instance of whisper is already running")

        val handle = initializeNative(modelPath, language, useGpu)
        if (handle == 0L) return false

        if (!_isInitialized.compareAndSet(expectedValue = false, newValue = true)) {
            destroyNative(handle)
            throw IllegalStateException("NativeWhisper was initialized on another thread.")
        }
        nativeHandle.store(handle)
        return true
    }

    /**
     * Normalizes 16-bit PCM short audio samples and passes them to native memory zero-copy.
     */
    fun processSamples(audioFrame: ShortArray): Boolean {

        val isInit = _isInitialized.load()
        if (!isInit) throw IllegalStateException("Whisper is not instantiated")

        if (audioFrame.size >= MAX_AUDIO_SAMPLES)
            throw IllegalArgumentException("Audio sample count (${audioFrame.size}) exceeds maximum buffer limit ($MAX_AUDIO_SAMPLES).")

        _floatBuffer.clear()
        for (i in audioFrame.indices) {
            _floatBuffer.put(audioFrame[i] * FLOAT_MULTIPLIER)
        }
        val handle = nativeHandle.load()
        return processNativeDirectBuffer(handle, audioBuffer, audioFrame.size)
    }

    /**
     * Retrieves the current transcribed text segments or state from C++.
     */
    fun readState(): WhisperState? {
        if (!_isInitialized.load()) return null
        return readStateNative(nativeHandle.load())
    }

    /**
     * Retrieves any native error code logged during processing.
     */
    fun readError(): WhisperErrorCode? {
        if (!_isInitialized.load()) return null
        val code = readErrorNative(nativeHandle.load())
        return WhisperErrorCode.fromCode(code)
    }

    override fun close() {
        if (_isInitialized.compareAndSet(expectedValue = true, newValue = false)) {
            val handle = nativeHandle.exchange(0L)
            if (handle != 0L) destroyNative(handle)
        } else {
            Log.w(TAG, "Close ignored: NativeWhisper was not initialized or already closed.")
        }
    }

    internal val isInitialized: Boolean
        get() = _isInitialized.load()

    // Native JNI functions
    private external fun initializeNative(modelPath: String, language: String, useGpu: Boolean): Long

    private external fun processNativeDirectBuffer(handle: Long, buffer: ByteBuffer, length: Int): Boolean
    private external fun readStateNative(handle: Long): WhisperState?
    private external fun readErrorNative(handle: Long): Int
    private external fun destroyNative(handle: Long)

    companion object {

        private const val FLOAT_MULTIPLIER = 1.0f / 32768.0f
        private const val MAX_AUDIO_SAMPLES = 16000 * 30

        init {
            System.loadLibrary("native_transcriptions")
        }
    }
}
