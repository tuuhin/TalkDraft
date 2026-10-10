package com.sam.talkdraft.transcription.domain

internal interface ITextPunctuator : AutoCloseable {
    suspend fun setupModel(modelPath: String): Result<Boolean>
    fun processText(input: String): String?
}
