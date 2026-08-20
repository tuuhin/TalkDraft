package com.sam.talkdraft.database.converters

import androidx.room3.ColumnTypeConverter
import androidx.room3.ProvidedColumnTypeConverter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

@ProvidedColumnTypeConverter
internal class DurationToLongConvertor {

    @ColumnTypeConverter
    fun fromDuration(from: Duration): Long = from.toLong(DurationUnit.MILLISECONDS)

    @ColumnTypeConverter
    fun toDuration(from: Long): Duration = from.milliseconds
}
