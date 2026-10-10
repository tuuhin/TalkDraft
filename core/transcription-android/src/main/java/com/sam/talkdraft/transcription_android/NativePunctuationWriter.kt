package com.sam.talkdraft.transcription_android

import com.sam.talkdraft.transcription_android.punctuation.JniPunctuationConfig
import com.sam.talkdraft.transcription_android.punctuation.JniTextPunctuationResult
import dalvik.annotation.optimization.FastNative
import java.util.Locale
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndUpdate

@OptIn(ExperimentalAtomicApi::class)
class NativePunctuationWriter : AutoCloseable {

    private val _nativeHandle = AtomicLong(0L)
    private val _isInitialized = AtomicBoolean(false)

    fun initialize(modelPath: String, vocalPath: String): Boolean {

        if (_isInitialized.load()) throw IllegalStateException("Punctuation selector cannot run. Close it to continue.")

        val config = JniPunctuationConfig(modelPath = modelPath, vocabPath = vocalPath)
        val handle = initInstance(config)

        if (handle <= 0L) throw IllegalStateException("Failed to initialize punctuation provider. Handle: $handle")

        if (!_isInitialized.compareAndSet(expectedValue = false, newValue = true)) {
            destroyNative(handle)
            throw IllegalStateException("Punctuation writer was initialized on another thread.")
        }

        _nativeHandle.store(handle)
        return true
    }


    fun processText(inputText: String): String? {
        if (inputText.isEmpty()) return null
        if (!_isInitialized.load()) throw IllegalStateException("Punctuations writer is not ready initialize it to continue")

        val handle = _nativeHandle.load()
        check(handle > 0L) { "Punctuation writer handle not found " }
        // need to pass lower case otherwise it will not work
        val lowerCaseInput = inputText.lowercase(Locale.ENGLISH)
        val result = processText(handle, lowerCaseInput)
        return result.text
    }

    override fun close() {
        if (!_isInitialized.compareAndSet(expectedValue = true, newValue = false)) return
        val handle = _nativeHandle.fetchAndUpdate { 0L }
        if (handle != 0L) destroyNative(handle)
    }

    internal val isInitialized: Boolean
        get() = _isInitialized.load()

    @Throws(IllegalArgumentException::class)
    private external fun initInstance(config: JniPunctuationConfig): Long

    @FastNative
    private external fun processText(handle: Long, input: String): JniTextPunctuationResult
    private external fun destroyNative(handle: Long)

    private companion object {
        init {
            System.loadLibrary("native_punctuations")
        }
    }
}
