package com.sam.talkdraft.database.converters

import androidx.room3.ColumnTypeConverter
import androidx.room3.ProvidedColumnTypeConverter
import com.sam.talkdraft.database.enums.DBRemoteModelStatus

@ProvidedColumnTypeConverter
internal class RemoteModelStatusConverter {

    @ColumnTypeConverter
    fun fromStatus(status: DBRemoteModelStatus): String = status.code

    @ColumnTypeConverter
    fun toStatus(code: String?): DBRemoteModelStatus = DBRemoteModelStatus.fromCode(code)
}
