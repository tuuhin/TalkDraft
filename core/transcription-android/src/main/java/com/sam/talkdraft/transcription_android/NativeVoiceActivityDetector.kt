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

private const val TAG = "NATIVE_VOICE_ACTIVITY_DETECTOR"

@OptIn(ExperimentalAtomicApi::class)
class NativeVoiceActivityDetector : AutoCloseable {

    private val _nativeHandle = AtomicLong(0L)
    private val _isInitialized = AtomicBoolean(false)

    private val frameBuffer: ByteBuffer = ByteBuffer
        .allocateDirect(SILERO_SAMPLE_COUNT * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())

    private val floatBuffer: FloatBuffer = frameBuffer.asFloatBuffer()

    fun initialize(
        assets: AssetManager,
        assetName: String = MODEL_NAME,
        sampleRate: Int = 16000,
        threshold: Float = 0.5f,
    ): Boolean {
        require(sampleRate == 16000 || sampleRate == 8000) { "VAD requires 8kHz or 16kHz sample rate." }

        val isInit = _isInitialized.load()
        if (isInit) throw IllegalStateException("Voice detector is already initialized close it to continue")

        val handle = initializeNativeFromAssets(assets, assetName, sampleRate, threshold)
        if (handle == 0L) return false

        if (!_isInitialized.compareAndSet(expectedValue = false, newValue = true)) {
            destroyNative(handle)
            throw IllegalStateException("VoiceActivityDetector was initialized on another thread.")
        }
        _nativeHandle.store(handle)
        return true
    }

    /**
     * Initializes the native VAD model using a file path.
     */
    fun initialize(modelPath: String, sampleRate: Int = 16000, threshold: Float = 0.5f): Boolean {
        require(sampleRate == 16000 || sampleRate == 8000) { "VAD requires 8kHz or 16kHz sample rate." }

        val isInit = _isInitialized.load()
        if (isInit) throw IllegalStateException("Voice detector is already initialized close it to continue")

        val handle = initializeNative(modelPath, sampleRate, threshold)
        if (handle == 0L) return false

        if (!_isInitialized.compareAndSet(expectedValue = false, newValue = true)) {
            destroyNative(handle)
            throw IllegalStateException("VoiceActivityDetector was initialized on another thread.")
        }

        _nativeHandle.store(handle)
        return true
    }

    /**
     * Processes raw 16-bit PCM audio samples.
     */
    fun processFrame(audioFrame: ShortArray): VoiceDetectionProbability {
        val isInit = _isInitialized.load()
        if (!isInit) throw IllegalStateException("Voice recorder is not initialized make sure its done first")

        // Copy short samples into direct byte buffe    r to prevent JNI allocation overhead
        floatBuffer.clear()
        for (i in audioFrame.indices)
            floatBuffer.put(audioFrame[i] * FLOAT_MULTIPLIER)

        val handle = _nativeHandle.load()

        val probability = processNativeDirectBuffer(handle, frameBuffer, audioFrame.size)
        return VoiceDetectionProbability(probability)
    }

    /**
     * Resets the internal RNN/LSTM state buffers without destroying the ONNX session.
     */
    fun resetState() {
        if (!_isInitialized.load()) {
            Log.w(TAG, "VOICE DETECTOR WAS NOT INITIALIZED")
            return
        }
        val nativeHandle = _nativeHandle.load()
        resetStatesNative(nativeHandle)
    }

    override fun close() {
        if (!_isInitialized.load()) {
            Log.w(TAG, "VOICE DETECTOR WAS NOT INITIALIZED")
            return
        }
        val nativeHandle = _nativeHandle.fetchAndUpdate { 0L }
        destroyNative(nativeHandle)
        _isInitialized.compareAndSet(expectedValue = true, newValue = false)
    }

    internal val isInitialized: Boolean
        get() = _isInitialized.load()

    private external fun initializeNative(modelPath: String, sampleRate: Int, threshold: Float): Long
    private external fun initializeNativeFromAssets(
        assetsManager: AssetManager,
        assetName: String,
        sampleRate: Int,
        threshold: Float,
    ): Long

    private external fun processNativeDirectBuffer(handle: Long, buffer: ByteBuffer, length: Int): Float
    private external fun resetStatesNative(handle: Long)
    private external fun destroyNative(handle: Long)

    companion object {

        init {
            System.loadLibrary("native_transcriptions")
        }

        private const val FLOAT_MULTIPLIER = 0.00003051757f
        private const val SILERO_SAMPLE_COUNT = 1536

        const val MODEL_NAME = "silero_vad.onnx"

    }
}
