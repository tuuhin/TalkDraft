package com.sam.talkdraft.database.converters

import androidx.room3.ColumnTypeConverter
import androidx.room3.ProvidedColumnTypeConverter
import com.sam.talkdraft.database.enums.DBModelFamilyOption

@ProvidedColumnTypeConverter
internal class ModelFamilyOptionConverter {

    @ColumnTypeConverter
    fun fromStatus(status: DBModelFamilyOption): String = status.family

    @ColumnTypeConverter
    fun toStatus(code: String?): DBModelFamilyOption = DBModelFamilyOption.fromCode(code)
}
