package com.sam.talkdraft.transcription.data

import com.sam.talkdraft.common.platform.IPlatformCoroutineDispatchers
import com.sam.talkdraft.transcription.domain.ITextPunctuator
import com.sam.talkdraft.transcription.domain.exceptions.PunctuatorModelMissingException
import com.sam.talkdraft.transcription_android.NativePunctuationWriter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import org.koin.core.annotation.Factory

@Factory(binds = [ITextPunctuator::class])
actual class PlatformTextPunctuator(
    private val dispatchers: IPlatformCoroutineDispatchers,
) : ITextPunctuator {

    private val punctuator by lazy { NativePunctuationWriter() }
    private val fs = FileSystem.SYSTEM

    actual override suspend fun setupModel(modelPath: String): Result<Boolean> {
        return withContext(dispatchers.io) {
            try {
                val punctuatorModelPath = modelPath.toPath()

                // in case file is missing
                if (!fs.exists(punctuatorModelPath)) throw PunctuatorModelMissingException()

                val files = fs.listRecursively(punctuatorModelPath).toList()
                // find the model file
                val modelFile = files.firstOrNull { path ->
                    val name = path.name.lowercase()
                    name.endsWith(".onnx") || name.endsWith(".int8.onnx")
                } ?: throw PunctuatorModelMissingException()

                val vocabPath = files.firstOrNull { path ->
                    path.name.lowercase().endsWith(".vocab")
                } ?: throw PunctuatorModelMissingException()

                Result.success(punctuator.initialize(modelFile.toString(), vocabPath.toString()))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Result.failure(e)
            }
        }
    }

    actual override fun processText(input: String): String? {
        return try {
            punctuator.processText(input)
        } catch (_: Exception) {
            // dont care about the exception
            null
        }
    }


    actual override fun close() = punctuator.close()
}
