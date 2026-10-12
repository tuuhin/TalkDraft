package com.sam.talkdraft.transcription.ios

import co.touchlab.kermit.Logger
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSError
import swiftPMImport.com.sam.talkdraft.transcription.ios.core.transcription.ios.IosPunctuationProvider

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class, ExperimentalAtomicApi::class)
class IosNativePunctuationWriter {

    private val writer by lazy { IosPunctuationProvider() }

    private val _isInitialized = AtomicBoolean(false)

    fun initialize(modelPath: String, vocalPath: String): Boolean {

        if (_isInitialized.load()) throw IllegalStateException("Punctuation selector cannot run. Close it to continue.")

        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            val handle = writer.doInitWithModelPath(modelPath, true, vocalPath, error.ptr)
            if (error.value != null) {
                val message = error.value?.localizedDescription
                Logger.e(tag = TAG) { "FAILED TO PREPARE WRITER INSTANCE :$message" }
                return false
            }
            _isInitialized.compareAndSet(expectedValue = false, true)
            return handle
        }
    }


    fun processText(inputText: String): String? {
        if (inputText.isEmpty()) return null
        if (!_isInitialized.load()) throw IllegalStateException("Punctuations writer is not ready initialize it to continue")

        return memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            val result = writer.processText(inputText.lowercase(), error.ptr)
            if (error.value != null) {
                val message = error.value?.localizedDescription
                Logger.e(tag = TAG) { "FAILED TO PREPARE WRITER INSTANCE" }
                throw IllegalStateException(message ?: "Failed to prepare result")
            }
            result
        }
    }

    fun close() {
        if (!_isInitialized.compareAndSet(expectedValue = true, newValue = false)) return
        // just cleanup
        writer.cleanUp()
    }

    companion object {
        private const val TAG = "IosNativePunctuationWriter"
    }
}
