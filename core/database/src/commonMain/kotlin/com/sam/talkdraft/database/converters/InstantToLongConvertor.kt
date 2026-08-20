package com.sam.talkdraft.database.converters

import androidx.room3.ColumnTypeConverter
import androidx.room3.ProvidedColumnTypeConverter
import kotlin.time.Instant

@ProvidedColumnTypeConverter
internal class InstantToLongConvertor {

    @ColumnTypeConverter
    fun fromInstantToMillis(from: Instant): Long = from.toEpochMilliseconds()

    @ColumnTypeConverter
    fun toMillisFromInstant(from: Long): Instant = Instant.fromEpochMilliseconds(from)
}
