package com.sam.talkdraft.transcription_android

import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@OptIn(ExperimentalAtomicApi::class)
class NativeZipFormer : AutoCloseable {

    private val _nativeHandle = AtomicLong(0L)
    private val _isInitialized = AtomicBoolean(false)
    private val lock = Any()

    private val audioBuffer: ByteBuffer = ByteBuffer
        .allocateDirect(MAX_CHUNK_SAMPLES * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())

    private val _floatBuffer = audioBuffer.asFloatBuffer()

    val isInitialized: Boolean
        get() = _isInitialized.load()

    fun initialize(encoderPath: String, decoderPath: String, joinerPath: String, tokensPath: String): Boolean =
        synchronized(lock) {
            if (_isInitialized.load()) {
                Log.w(TAG, "An instance of NativeZipFormer is already running.")
                return false
            }

            val encoderFile = File(encoderPath)
            val decoderFile = File(decoderPath)
            val joinerFile = File(joinerPath)
            val tokensFile = File(tokensPath)

            require(encoderFile.exists()) { "Encoder file does not exist: $encoderPath" }
            require(decoderFile.exists()) { "Decoder file does not exist: $decoderPath" }
            require(joinerFile.exists()) { "Joiner file does not exist: $joinerPath" }
            require(tokensFile.exists()) { "Tokens file does not exist: $tokensPath" }

            val handle = initializeNative(encoderPath, decoderPath, joinerPath, tokensPath)
            if (handle == 0L) {
                Log.e(TAG, "Failed to initialize NativeZipFormer in native layer.")
                return false
            }

            if (!_isInitialized.compareAndSet(expectedValue = false, newValue = true)) {
                destroyNative(handle)
                throw IllegalStateException("NativeZipFormer was initialized concurrently on another thread.")
            }
            _nativeHandle.store(handle)
            Log.i(TAG, "NativeZipFormer initialized successfully.")
            return true
        }

    fun processFrame(audioFrame: ShortArray): String? = synchronized(lock) {
        if (!_isInitialized.load()) throw IllegalStateException("NativeZipFormer is not initialized")
        if (audioFrame.isEmpty()) return null

        val handle = _nativeHandle.load()
        check(handle != 0L) { "NativeZipFormer native handle is invalid." }
        var currentTranscript: String? = null

        var offset = 0
        while (offset < audioFrame.size) {

            val chunkLen = minOf(audioFrame.size - offset, MAX_CHUNK_SAMPLES)
            _floatBuffer.clear()
            for (i in 0 until chunkLen) {
                _floatBuffer.put(audioFrame[offset + i] * FLOAT_MULTIPLIER)
            }
            audioBuffer.position(0)
            audioBuffer.limit(chunkLen * Float.SIZE_BYTES)

            val text = processNativeDirectBuffer(handle, audioBuffer, chunkLen)
            if (text != null) {
                currentTranscript = text
            }
            offset += chunkLen
        }

        return currentTranscript
    }

    fun reset(): Unit = synchronized(lock) {
        if (!_isInitialized.load()) return
        val handle = _nativeHandle.load()
        if (handle != 0L) resetNative(handle)
    }

    override fun close(): Unit = synchronized(lock) {
        if (_isInitialized.compareAndSet(expectedValue = true, newValue = false)) {
            val handle = _nativeHandle.exchange(0L)
            if (handle != 0L) destroyNative(handle)
            Log.i(TAG, "NativeZipFormer closed.")
        } else {
            Log.w(TAG, "Close ignored: NativeZipFormer was not initialized or already closed.")
        }
    }

    private external fun initializeNative(
        encoderPath: String,
        decoderPath: String,
        joinerPath: String,
        tokenPath: String,
    ): Long

    private external fun processNativeDirectBuffer(handle: Long, buffer: ByteBuffer, length: Int): String?
    private external fun destroyNative(handle: Long)
    private external fun resetNative(handle: Long)

    companion object {
        private const val TAG = "NativeZipFormer"

        private const val FLOAT_MULTIPLIER = 1.0f / 32768.0f
        private const val MAX_CHUNK_SAMPLES = 16000 * 10 // 10 seconds of 16kHz audio per direct buffer chunk

        init {
            System.loadLibrary("native_zipformer")
        }
    }
}
