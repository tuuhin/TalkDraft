package com.sam.talkdraft.database.converters

import androidx.room3.ColumnTypeConverter
import androidx.room3.ProvidedColumnTypeConverter

@ProvidedColumnTypeConverter
class ListToStringConvertor {

    @ColumnTypeConverter
    fun fromListOfStringToCSV(values: List<String>): String = values.joinToString(",")

    @ColumnTypeConverter
    fun csvToStringList(text: String): List<String> = text.split(",")
}

