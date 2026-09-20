package com.sam.talkdraft.database.converters

import androidx.room3.ColumnTypeConverter
import androidx.room3.ProvidedColumnTypeConverter
import com.sam.talkdraft.database.enums.ProcessingType

@ProvidedColumnTypeConverter
internal class ProcessingTypeConverter {

    @ColumnTypeConverter
    fun fromType(type: ProcessingType): String = type.code

    @ColumnTypeConverter
    fun toType(code: String?): ProcessingType = ProcessingType.fromCode(code)
}
