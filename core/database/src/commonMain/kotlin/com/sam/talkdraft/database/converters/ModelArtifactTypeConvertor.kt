package com.sam.talkdraft.database.converters

import androidx.room3.ColumnTypeConverter
import androidx.room3.ProvidedColumnTypeConverter
import com.sam.talkdraft.database.enums.DBModelArtifactType

@ProvidedColumnTypeConverter
internal class ModelArtifactTypeConvertor {

    @ColumnTypeConverter
    fun fromStatus(status: DBModelArtifactType): String = status.code

    @ColumnTypeConverter
    fun toStatus(code: String?): DBModelArtifactType = DBModelArtifactType.fromCode(code)
}
