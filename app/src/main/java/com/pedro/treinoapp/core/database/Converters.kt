package com.pedro.treinoapp.core.database

import androidx.room.TypeConverter
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/**
 * Datas são gravadas como epoch day, instantes como epoch millis e dias da semana
 * no padrão ISO (1 = segunda … 7 = domingo).
 */
class Converters {

    @TypeConverter
    fun localDateToEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun instantToEpochMilli(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun epochMilliToInstant(epochMilli: Long?): Instant? = epochMilli?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun dayOfWeekToInt(dayOfWeek: DayOfWeek?): Int? = dayOfWeek?.value

    @TypeConverter
    fun intToDayOfWeek(value: Int?): DayOfWeek? = value?.let(DayOfWeek::of)
}
