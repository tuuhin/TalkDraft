package com.sam.talkdraft.transcription_android

import android.content.Context
import android.content.res.AssetManager
import com.sam.talkdraft.transcription_android.vad.AndroidVADResult
import com.sam.talkdraft.transcription_android.vad.AndroidVadConfig
import com.sam.talkdraft.transcription_android.vad.AndroidVadSegment
import com.sam.talkdraft.transcription_android.vad.JniVadSegment
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndUpdate
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

@OptIn(ExperimentalAtomicApi::class)
class NativeVoiceActivityDetector(private val context: Context) : AutoCloseable {

    private val _nativeHandle = AtomicLong(0L)
    private val _isInitialized = AtomicBoolean(false)
    private val _sampleRate = AtomicInt(0)

    // buffer size of 16
    private val _segmentChannel = Channel<AndroidVadSegment>(capacity = 16)
    val segments = _segmentChannel.receiveAsFlow()

    private val _audioByteBuffer = ByteBuffer
        .allocateDirect(MAX_SAMPLE_COUNT * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())

    private val floatBuffer = _audioByteBuffer.asFloatBuffer()

    fun initialize(assets: AssetManager, config: AndroidVadConfig = AndroidVadConfig()): Boolean {
        require(config.isValidSampleRate) { "VAD requires 8kHz or 16kHz sample rate." }

        if (_isInitialized.load()) throw IllegalStateException("Voice detector is already initialized close it to continue")

        val handle = initFromAssets(assets, context.cacheDir.absolutePath, MODEL_NAME, config)
        if (handle <= 0L) throw IllegalStateException("Failed to initiate a instance")

        if (!_isInitialized.compareAndSet(expectedValue = false, newValue = true)) {
            destroyNative(handle)
            throw IllegalStateException("VoiceActivityDetector was initialized on another thread.")
        }
        _nativeHandle.store(handle)
        return true
    }

    fun initialize(modelPath: String, config: AndroidVadConfig = AndroidVadConfig()): Boolean {
        require(config.isValidSampleRate) { "VAD requires 8kHz or 16kHz sample rate." }

        if (_isInitialized.load()) throw IllegalStateException("Voice detector is already initialized close it to continue")

        val handle = initNative(modelPath, config)
        if (handle <= 0L) throw IllegalStateException("Failed to initiate a instance")

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

        val sampleCount = audioFrame.size
        // if the sample rate is 0 or undefined set it to sample count
        _sampleRate.compareAndSet(0, sampleCount)

        floatBuffer.clear()
        for (i in audioFrame.indices)
            floatBuffer.put(audioFrame[i] * FLOAT_MULTIPLIER)

        floatBuffer.position(0)
        // limited to the sample count
        floatBuffer.limit(sampleCount * Float.SIZE_BYTES)

        val isSpeech = processNativeDirectBuffer(handle, _audioByteBuffer, sampleCount)
        drainNativeSegments(handle, sampleCount)

        return AndroidVADResult(isSpeech = isSpeech)
    }

    private fun drainNativeSegments(handle: Long, sampleRate: Int) {
        while (true) {
            // try to extract out the segment if any available
            val segment = popNativeSegmentFromSpeech(handle) ?: break

            if (segment.samples.isNotEmpty()) {
                val startDuration = (segment.startSample / sampleRate).toDuration(DurationUnit.SECONDS)
                val endDuration = (segment.endSample / sampleRate).toDuration(DurationUnit.SECONDS)
                val vadSegment = AndroidVadSegment(
                    startSample = startDuration,
                    endSample = endDuration,
                    samples = segment.samples,
                )
                _segmentChannel.trySend(vadSegment)
            }
        }
    }

    fun resetState() {
        if (!_isInitialized.load()) return
        val handle = _nativeHandle.load()
        if (handle == 0L) return
        // reset the internal state
        resetStatesNative(handle)

        while (true) {
            val result = _segmentChannel.tryReceive()
            // flush out everything from the channel
            if (!result.isSuccess) break
        }
    }

    fun flushSpeechSegments() {
        check(_isInitialized.load()) { "Voice detector is not initialized." }
        val handle = _nativeHandle.load()
        check(handle != 0L) { "Voice detector native handle is invalid." }
        flushNative(handle)
        drainNativeSegments(handle, _sampleRate.load())
    }

    override fun close() {
        if (!_isInitialized.compareAndSet(expectedValue = true, newValue = false)) return
        val handle = _nativeHandle.fetchAndUpdate { 0L }
        if (handle != 0L) destroyNative(handle)
        _segmentChannel.close()
        _sampleRate.store(0)
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

    private external fun processNativeDirectBuffer(handle: Long, buffer: ByteBuffer, length: Int): Boolean
    private external fun resetStatesNative(handle: Long)
    private external fun destroyNative(handle: Long)

    // use for direct speech buffer extraction
    private external fun popNativeSegmentFromSpeech(handle: Long): JniVadSegment?
    private external fun flushNative(handle: Long)

    companion object {

        init {
            System.loadLibrary("native_vad")
        }

        private const val FLOAT_MULTIPLIER = 1.0f / Short.MAX_VALUE
        private const val MAX_SAMPLE_COUNT = 16_000

        // using the quantized model
        internal const val MODEL_NAME = "silero_vad.int8.onnx"
    }
}
