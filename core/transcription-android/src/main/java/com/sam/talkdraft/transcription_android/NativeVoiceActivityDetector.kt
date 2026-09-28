package com.sam.talkdraft.transcription_android

import android.content.Context
import android.content.res.AssetManager
import com.sam.talkdraft.transcription_android.models.AndroidVADResult
import com.sam.talkdraft.transcription_android.models.AndroidVadConfig
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndUpdate

@OptIn(ExperimentalAtomicApi::class)
class NativeVoiceActivityDetector(private val context: Context) : AutoCloseable {

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
        config: AndroidVadConfig = AndroidVadConfig(),
    ): Boolean {
        require(config.sampleRate == 16000 || config.sampleRate == 8000) { "VAD requires 8kHz or 16kHz sample rate." }

        if (_isInitialized.load()) throw IllegalStateException("Voice detector is already initialized close it to continue")

        val handle = initFromAssets(assets, context.cacheDir.absolutePath, MODEL_NAME, config)
        if (handle == 0L) return false

        if (!_isInitialized.compareAndSet(expectedValue = false, newValue = true)) {
            destroyNative(handle)
            throw IllegalStateException("VoiceActivityDetector was initialized on another thread.")
        }
        _nativeHandle.store(handle)
        return true
    }

    fun initialize(modelPath: String, config: AndroidVadConfig = AndroidVadConfig()): Boolean {
        require(config.sampleRate == 16000 || config.sampleRate == 8000) { "VAD requires 8kHz or 16kHz sample rate." }

        if (_isInitialized.load()) throw IllegalStateException("Voice detector is already initialized close it to continue")

        val handle = initNative(modelPath, config)
        if (handle == 0L) return false

        if (!_isInitialized.compareAndSet(expectedValue = false, newValue = true)) {
            destroyNative(handle)
            throw IllegalStateException("VoiceActivityDetector was initialized on another thread.")
        }

        _nativeHandle.store(handle)
        return true
    }


    fun processFrame(audioFrame: ShortArray): AndroidVADResult {
        if (!_isInitialized.load()) throw IllegalStateException("Voice recorder is not initialized make sure its done first")
        if (audioFrame.isEmpty()) return AndroidVADResult(false)

        val handle = _nativeHandle.load()
        check(handle != 0L) { "Voice detector native handle is invalid." }
        appendSamples(audioFrame)

        var isSpeech = false

        while (pendingSampleCount >= SILERO_SAMPLE_COUNT) {
            val result = runNativeInference(handle)
            if (result.isSpeech) isSpeech = true
        }

        return AndroidVADResult(isSpeech = isSpeech)
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

    private fun runNativeInference(handle: Long): AndroidVADResult {
        floatBuffer.clear()
        for (i in 0 until SILERO_SAMPLE_COUNT) {
            val buf = pendingSamples[i] * FLOAT_MULTIPLIER
            floatBuffer.put(buf)
        }
        frameBuffer.position(0)
        frameBuffer.limit(SILERO_SAMPLE_COUNT * Float.SIZE_BYTES)

        val isSpeech = processNativeDirectBuffer(handle, frameBuffer, SILERO_SAMPLE_COUNT)

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

        return AndroidVADResult(isSpeech)
    }

    fun resetState() {
        if (!_isInitialized.load()) return
        resetStatesNative(_nativeHandle.load())
        pendingSampleCount = 0   // also drop any partial PCM carry-over on reset
    }

    fun popSpeechSegments(): Sequence<FloatArray> {
        check(_isInitialized.load()) { "Voice detector is not initialized." }

        val handle = _nativeHandle.load()
        check(handle != 0L) { "Voice detector native handle is invalid." }

        return sequence {
            while (true) {
                val segment = popNativeSegmentFromSpeech(handle) ?: break
                if (segment.isNotEmpty()) yield(segment)
            }
        }
    }

    fun flushSpeechSegments(): Sequence<FloatArray> {
        check(_isInitialized.load()) { "Voice detector is not initialized." }
        val handle = _nativeHandle.load()
        check(handle != 0L) { "Voice detector native handle is invalid." }
        pendingSampleCount = 0
        flushNative(handle)
        return popSpeechSegments()
    }

    override fun close() {
        if (!_isInitialized.compareAndSet(expectedValue = true, newValue = false)) return
        val handle = _nativeHandle.fetchAndUpdate { 0L }
        if (handle != 0L) destroyNative(handle)
        pendingSampleCount = 0
    }

    internal val isInitialized: Boolean
        get() = _isInitialized.load()

    private external fun initNative(modelPath: String, config: AndroidVadConfig): Long
    private external fun initFromAssets(
        manager: AssetManager,
        cacheDir: String,
        modelName: String,
        config: AndroidVadConfig,
    ): Long

    private external fun processNativeDirectBuffer(
        handle: Long,
        buffer: ByteBuffer,
        length: Int,
    ): Boolean

    private external fun resetStatesNative(handle: Long)
    private external fun destroyNative(handle: Long)

    // use for direct speech buffer extraction
    private external fun popNativeSegmentFromSpeech(handle: Long): FloatArray?
    private external fun flushNative(handle: Long)

    companion object {

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

        // using the quantized model
        internal const val MODEL_NAME = "silero_vad.int8.onnx"
    }
}
