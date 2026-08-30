package com.sam.talkdraft.designsystem.utils.formatter

import org.koin.core.annotation.Singleton

@Singleton(binds = [IBytesSizeFormatter::class])
internal expect class ByteSizeFormatterImpl : IBytesSizeFormatter {
    override fun formatToString(bytes: Long, style: BytesFormatterStyle): String
}
