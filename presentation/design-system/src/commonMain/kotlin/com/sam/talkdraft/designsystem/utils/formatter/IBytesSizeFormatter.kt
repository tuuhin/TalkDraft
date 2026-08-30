package com.sam.talkdraft.designsystem.utils.formatter

interface IBytesSizeFormatter {
    fun formatToString(bytes: Long, style: BytesFormatterStyle = BytesFormatterStyle.FILE_SIZE): String
}
