package com.sam.talkdraft.database.converters

import androidx.room3.ColumnTypeConverter
import androidx.room3.ProvidedColumnTypeConverter
import com.sam.talkdraft.database.enums.DBModelTranscriptionType

@ProvidedColumnTypeConverter
internal class TranscriptionTypeConvertor {

    @ColumnTypeConverter
    fun fromStatus(status: DBModelTranscriptionType): String = status.code

    @ColumnTypeConverter
    fun toStatus(code: String?): DBModelTranscriptionType = DBModelTranscriptionType.fromCode(code)
}
