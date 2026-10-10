package com.sam.talkdraft.transcription.data

import com.sam.talkdraft.transcription.domain.ITextPunctuator
import org.koin.core.annotation.Factory

@Factory(binds = [ITextPunctuator::class])
expect class PlatformTextPunctuator : ITextPunctuator {
    override suspend fun setupModel(modelPath: String): Result<Boolean>
    override fun processText(input: String): String?
    override fun close()
}
