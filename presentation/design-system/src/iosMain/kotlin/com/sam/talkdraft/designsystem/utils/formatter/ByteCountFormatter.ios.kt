package com.sam.talkdraft.designsystem.utils.formatter

import org.koin.core.annotation.Singleton
import platform.Foundation.NSByteCountFormatter
import platform.Foundation.NSByteCountFormatterCountStyleBinary
import platform.Foundation.NSByteCountFormatterCountStyleFile

@Singleton(binds = [IBytesSizeFormatter::class])
internal actual class ByteSizeFormatterImpl : IBytesSizeFormatter {

    actual override fun formatToString(bytes: Long, style: BytesFormatterStyle): String {

        return when (style) {
            BytesFormatterStyle.FILE_SIZE ->
                NSByteCountFormatter.stringFromByteCount(bytes, NSByteCountFormatterCountStyleFile)

            BytesFormatterStyle.FILE_SHORT_SIZE ->
                NSByteCountFormatter.stringFromByteCount(bytes, NSByteCountFormatterCountStyleBinary)
        }
    }
}
