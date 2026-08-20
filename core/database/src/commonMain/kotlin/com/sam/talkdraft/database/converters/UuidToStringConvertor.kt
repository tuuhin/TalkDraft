package com.sam.talkdraft.database.converters

import androidx.room3.ColumnTypeConverter
import androidx.room3.ProvidedColumnTypeConverter
import kotlin.uuid.Uuid

@ProvidedColumnTypeConverter
internal class UuidToStringConvertor {

    @ColumnTypeConverter
    fun fromUUIDToText(uuid: Uuid): String = uuid.toHexString()

    @ColumnTypeConverter
    fun fromTextToUUID(text: String): Uuid = Uuid.parseHex(text)
}
