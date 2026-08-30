package com.sam.talkdraft.designsystem.utils.formatter

import android.content.Context
import android.text.format.Formatter
import org.koin.core.annotation.Singleton

@Singleton(binds = [IBytesSizeFormatter::class])
internal actual class ByteSizeFormatterImpl(private val context: Context) : IBytesSizeFormatter {
    actual override fun formatToString(bytes: Long, style: BytesFormatterStyle): String {
        return when (style) {
            BytesFormatterStyle.FILE_SIZE -> Formatter.formatFileSize(context, bytes)
            BytesFormatterStyle.FILE_SHORT_SIZE -> Formatter.formatShortFileSize(context, bytes)
        }
    }
}
