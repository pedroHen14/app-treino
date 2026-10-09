package com.pedro.treinoapp.core.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `local date round trips through epoch day`() {
        val date = LocalDate.of(2026, 10, 9)

        val epochDay = converters.localDateToEpochDay(date)

        assertEquals(date.toEpochDay(), epochDay)
        assertEquals(date, converters.epochDayToLocalDate(epochDay))
    }

    @Test
    fun `epoch day zero is 1970-01-01`() {
        assertEquals(LocalDate.of(1970, 1, 1), converters.epochDayToLocalDate(0L))
    }

    @Test
    fun `date before 1970 maps to negative epoch day`() {
        val date = LocalDate.of(1969, 12, 31)

        assertEquals(-1L, converters.localDateToEpochDay(date))
        assertEquals(date, converters.epochDayToLocalDate(-1L))
    }

    @Test
    fun `instant round trips through epoch millis`() {
        val instant = Instant.ofEpochMilli(1_791_567_000_123L)

        val millis = converters.instantToEpochMilli(instant)

        assertEquals(1_791_567_000_123L, millis)
        assertEquals(instant, converters.epochMilliToInstant(millis))
    }

    @Test
    fun `day of week uses ISO numbering`() {
        assertEquals(1, converters.dayOfWeekToInt(DayOfWeek.MONDAY))
        assertEquals(7, converters.dayOfWeekToInt(DayOfWeek.SUNDAY))
        assertEquals(DayOfWeek.MONDAY, converters.intToDayOfWeek(1))
        assertEquals(DayOfWeek.SUNDAY, converters.intToDayOfWeek(7))
    }

    @Test
    fun `null values stay null`() {
        assertNull(converters.localDateToEpochDay(null))
        assertNull(converters.epochDayToLocalDate(null))
        assertNull(converters.instantToEpochMilli(null))
        assertNull(converters.epochMilliToInstant(null))
        assertNull(converters.dayOfWeekToInt(null))
        assertNull(converters.intToDayOfWeek(null))
    }
}
