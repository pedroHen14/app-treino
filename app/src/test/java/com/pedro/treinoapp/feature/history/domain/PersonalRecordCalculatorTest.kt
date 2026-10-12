package com.pedro.treinoapp.feature.history.domain

import com.pedro.treinoapp.feature.history.domain.model.RecordMark
import com.pedro.treinoapp.feature.history.domain.model.RecordType.MAX_REPS_AT_WEIGHT
import com.pedro.treinoapp.feature.history.domain.model.RecordType.MAX_WEIGHT
import com.pedro.treinoapp.feature.history.domain.model.RecordedSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PersonalRecordCalculatorTest {

    private val day1 = LocalDate.of(2026, 10, 5)
    private val day2 = LocalDate.of(2026, 10, 8)

    private fun set(id: Long, weightGrams: Int, reps: Int, date: LocalDate = day1, sessionId: Long = 1) =
        RecordedSet(setId = id, sessionId = sessionId, date = date, weightGrams = weightGrams, reps = reps)

    @Test
    fun `first execution of an exercise is not a record`() {
        val analysis = PersonalRecordCalculator.analyze(listOf(set(1, 60_000, 10)))

        assertTrue(analysis.recordsBySet.isEmpty())
        assertEquals(RecordMark(60_000, 10, day1), analysis.current.maxWeight)
    }

    @Test
    fun `heavier weight than ever before is a weight record`() {
        val analysis = PersonalRecordCalculator.analyze(
            listOf(set(1, 60_000, 10), set(2, 62_500, 8, day2, sessionId = 2)),
        )

        assertEquals(mapOf(2L to setOf(MAX_WEIGHT)), analysis.recordsBySet)
        assertEquals(RecordMark(62_500, 8, day2), analysis.current.maxWeight)
    }

    @Test
    fun `more reps with the same weight is a reps record`() {
        val analysis = PersonalRecordCalculator.analyze(
            listOf(set(1, 60_000, 10), set(2, 60_000, 11, day2, sessionId = 2)),
        )

        assertEquals(mapOf(2L to setOf(MAX_REPS_AT_WEIGHT)), analysis.recordsBySet)
    }

    @Test
    fun `tying the best mark is not a record`() {
        val analysis = PersonalRecordCalculator.analyze(
            listOf(set(1, 60_000, 10), set(2, 60_000, 10, day2, sessionId = 2)),
        )

        assertTrue(analysis.recordsBySet.isEmpty())
    }

    @Test
    fun `sets are compared with earlier sets of the same session`() {
        val analysis = PersonalRecordCalculator.analyze(
            listOf(set(1, 60_000, 10), set(2, 62_500, 8), set(3, 62_500, 9)),
        )

        assertEquals(mapOf(2L to setOf(MAX_WEIGHT), 3L to setOf(MAX_REPS_AT_WEIGHT)), analysis.recordsBySet)
    }

    @Test
    fun `first time with a lighter weight is not a record`() {
        val analysis = PersonalRecordCalculator.analyze(
            listOf(set(1, 60_000, 10), set(2, 50_000, 15, day2, sessionId = 2)),
        )

        assertTrue(analysis.recordsBySet.isEmpty())
        assertEquals(
            listOf(RecordMark(60_000, 10, day1), RecordMark(50_000, 15, day2)),
            analysis.current.bestRepsByWeight,
        )
    }

    @Test
    fun `bodyweight sets count reps records at zero weight`() {
        val analysis = PersonalRecordCalculator.analyze(
            listOf(set(1, 0, 8), set(2, 0, 10, day2, sessionId = 2)),
        )

        assertEquals(mapOf(2L to setOf(MAX_REPS_AT_WEIGHT)), analysis.recordsBySet)
    }

    @Test
    fun `a set can break both records only when the weight was already done`() {
        // 62,5 kg nunca foi feito antes: bate carga, mas não há marca de reps com essa carga.
        val analysis = PersonalRecordCalculator.analyze(listOf(set(1, 60_000, 10), set(2, 62_500, 12)))

        assertEquals(setOf(MAX_WEIGHT), analysis.recordsBySet[2L])
    }

    @Test
    fun `current records keep the first set that reached the max weight`() {
        val analysis = PersonalRecordCalculator.analyze(
            listOf(set(1, 62_500, 8), set(2, 62_500, 9, day2, sessionId = 2)),
        )

        assertEquals(RecordMark(62_500, 8, day1), analysis.current.maxWeight)
        assertEquals(listOf(RecordMark(62_500, 9, day2)), analysis.current.bestRepsByWeight)
    }

    @Test
    fun `no sets means no records`() {
        val analysis = PersonalRecordCalculator.analyze(emptyList())

        assertTrue(analysis.recordsBySet.isEmpty())
        assertNull(analysis.current.maxWeight)
        assertTrue(analysis.current.bestRepsByWeight.isEmpty())
    }
}
