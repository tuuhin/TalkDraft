package com.sam.talkdraft.transcription_android

import android.content.res.AssetManager
import android.util.Log
import com.sam.talkdraft.transcription_android.models.VoiceDetectionProbability
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndUpdate

@OptIn(ExperimentalAtomicApi::class)
class NativeVoiceActivityDetector : AutoCloseable {

    private val _nativeHandle = AtomicLong(0L)
    private val _isInitialized = AtomicBoolean(false)

    private val frameBuffer: ByteBuffer = ByteBuffer
        .allocateDirect(SILERO_SAMPLE_COUNT * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
    private val floatBuffer: FloatBuffer = frameBuffer.asFloatBuffer()

    private val pendingSamples = ShortArray(MAX_PENDING_SAMPLE_COUNT)
    private var pendingSampleCount = 0

    fun initialize(
        assets: AssetManager,
        assetName: String = MODEL_NAME,
        sampleRate: Int = 16000,
    ): Boolean {
        require(sampleRate == 16000 || sampleRate == 8000) { "VAD requires 8kHz or 16kHz sample rate." }

        if (_isInitialized.load()) throw IllegalStateException("Voice detector is already initialized close it to continue")

        val handle = initializeNativeFromAssets(assets, assetName, sampleRate)
        if (handle == 0L) return false

        if (!_isInitialized.compareAndSet(expectedValue = false, newValue = true)) {
            destroyNative(handle)
            throw IllegalStateException("VoiceActivityDetector was initialized on another thread.")
        }
        _nativeHandle.store(handle)
        return true
    }

    fun initialize(modelPath: String, sampleRate: Int = 16000): Boolean {
        require(sampleRate == 16000 || sampleRate == 8000) { "VAD requires 8kHz or 16kHz sample rate." }

        if (_isInitialized.load()) throw IllegalStateException("Voice detector is already initialized close it to continue")

        val handle = initializeNative(modelPath, sampleRate)
        if (handle == 0L) return false

        if (!_isInitialized.compareAndSet(expectedValue = false, newValue = true)) {
            destroyNative(handle)
            throw IllegalStateException("VoiceActivityDetector was initialized on another thread.")
        }

        _nativeHandle.store(handle)
        return true
    }


    fun processFrame(audioFrame: ShortArray): VoiceDetectionProbability {
        if (!_isInitialized.load()) throw IllegalStateException("Voice recorder is not initialized make sure its done first")
        if (audioFrame.isEmpty()) return VoiceDetectionProbability(0f)

        val handle = _nativeHandle.load()
        check(handle != 0L) { "Voice detector native handle is invalid." }

        appendSamples(audioFrame)

        var maxProbability = 0f
        while (pendingSampleCount >= SILERO_SAMPLE_COUNT) {
            val probability = runNativeInference(handle)
            if (probability > maxProbability) maxProbability = probability
        }

        return VoiceDetectionProbability(maxProbability)
    }

    private fun appendSamples(audioFrame: ShortArray) {
        var sourceIndex = 0
        while (sourceIndex < audioFrame.size) {
            val spaceAvailable = pendingSamples.size - pendingSampleCount
            if (spaceAvailable == 0)
                throw IllegalStateException(
                    "Pending sample buffer is full. pendingSampleCount=$pendingSampleCount, incoming=${audioFrame.size}",
                )
            val samplesToCopy = minOf(spaceAvailable, audioFrame.size - sourceIndex)
            audioFrame.copyInto(
                destination = pendingSamples,
                destinationOffset = pendingSampleCount,
                startIndex = sourceIndex,
                endIndex = sourceIndex + samplesToCopy,
            )
            pendingSampleCount += samplesToCopy
            sourceIndex += samplesToCopy
        }
    }

    private fun runNativeInference(handle: Long): Float {
        floatBuffer.clear()
        for (i in 0 until SILERO_SAMPLE_COUNT) {
            val buf = pendingSamples[i] * FLOAT_MULTIPLIER
            floatBuffer.put(buf)
        }
        frameBuffer.position(0)
        frameBuffer.limit(SILERO_SAMPLE_COUNT * Float.SIZE_BYTES)

        val probability = processNativeDirectBuffer(handle, frameBuffer, SILERO_SAMPLE_COUNT)

        // Shift remaining samples to the front.
        val remaining = pendingSampleCount - SILERO_SAMPLE_COUNT
        if (remaining > 0) {
            pendingSamples.copyInto(
                destination = pendingSamples,
                destinationOffset = 0,
                startIndex = SILERO_SAMPLE_COUNT,
                endIndex = pendingSampleCount,
            )
        }
        pendingSampleCount = remaining.coerceAtLeast(0)

        return probability
    }

    fun resetState() {
        if (!_isInitialized.load()) {
            Log.w(TAG, "VOICE DETECTOR WAS NOT INITIALIZED")
            return
        }
        resetStatesNative(_nativeHandle.load())
        pendingSampleCount = 0   // also drop any partial PCM carry-over on reset
    }

    override fun close() {
        if (!_isInitialized.compareAndSet(expectedValue = true, newValue = false)) {
            Log.w(TAG, "VOICE DETECTOR WAS NOT INITIALIZED")
            return
        }
        val handle = _nativeHandle.fetchAndUpdate { 0L }
        if (handle != 0L) destroyNative(handle)
        pendingSampleCount = 0
    }

    internal val isInitialized: Boolean
        get() = _isInitialized.load()

    private external fun initializeNative(modelPath: String, sampleRate: Int): Long
    private external fun initializeNativeFromAssets(
        assetsManager: AssetManager,
        assetName: String,
        sampleRate: Int,
    ): Long

    private external fun processNativeDirectBuffer(handle: Long, buffer: ByteBuffer, length: Int): Float
    private external fun resetStatesNative(handle: Long)
    private external fun destroyNative(handle: Long)

    companion object {
        private const val TAG = "NativeVAD"

        init {
	        System.loadLibrary("native_vad")
        }

        private const val FLOAT_MULTIPLIER = 1.0f / Short.MAX_VALUE

        // The model's actual required window size — this is what worked for you
        // at 640 samples/call. Confirm against your model export if unsure.
        private const val SILERO_SAMPLE_COUNT = 640

        // Generous headroom so large caller chunks (e.g. 16000 samples = 1s @16kHz)
        // never overflow the carry-over buffer between calls.
        private const val MAX_PENDING_SAMPLE_COUNT = 32_000

        const val MODEL_NAME = "silero_vad.onnx"
    }
}
