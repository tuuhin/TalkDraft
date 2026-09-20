package com.sam.talkdraft.database.converters

import androidx.room3.ColumnTypeConverter
import androidx.room3.ProvidedColumnTypeConverter
import com.sam.talkdraft.database.enums.DBModelDownloadStatus

@ProvidedColumnTypeConverter
internal class ModelDownloadStatusConverter {

    @ColumnTypeConverter
    fun fromStatus(status: DBModelDownloadStatus): String = status.code

    @ColumnTypeConverter
    fun toStatus(code: String?): DBModelDownloadStatus = DBModelDownloadStatus.fromCode(code)
}
