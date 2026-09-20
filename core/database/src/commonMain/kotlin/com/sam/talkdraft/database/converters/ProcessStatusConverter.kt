package com.sam.talkdraft.database.converters

import androidx.room3.ColumnTypeConverter
import androidx.room3.ProvidedColumnTypeConverter
import com.sam.talkdraft.database.enums.ProcessStatus

@ProvidedColumnTypeConverter
internal class ProcessStatusConverter {

    @ColumnTypeConverter
    fun fromStatus(status: ProcessStatus): String = status.code

    @ColumnTypeConverter
    fun toStatus(code: String?): ProcessStatus = ProcessStatus.fromCode(code)
}
