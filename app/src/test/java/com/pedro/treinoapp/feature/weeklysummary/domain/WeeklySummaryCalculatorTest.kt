package com.pedro.treinoapp.feature.weeklysummary.domain

import com.pedro.treinoapp.feature.history.domain.recordedSet
import com.pedro.treinoapp.feature.history.domain.model.RecordedSet
import com.pedro.treinoapp.feature.session.domain.TODAY
import com.pedro.treinoapp.feature.session.domain.model.SessionDay
import com.pedro.treinoapp.feature.weeklysummary.domain.model.DayStatus
import com.pedro.treinoapp.feature.weeklysummary.domain.model.DayStatus.DONE
import com.pedro.treinoapp.feature.weeklysummary.domain.model.DayStatus.MADE_UP
import com.pedro.treinoapp.feature.weeklysummary.domain.model.DayStatus.MISSED
import com.pedro.treinoapp.feature.weeklysummary.domain.model.DayStatus.PENDING
import com.pedro.treinoapp.feature.weeklysummary.domain.model.DayStatus.REST
import com.pedro.treinoapp.feature.weeklysummary.domain.model.WeekDay
import com.pedro.treinoapp.feature.weeklysummary.domain.model.WeeklySummary
import com.pedro.treinoapp.feature.workout.domain.model.ScheduleDay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.LocalDate

/** Hoje é quinta, 15/10/2026. Agenda: Upper (1) seg e qui, Lower (2) ter e sex, descanso no resto. */
class WeeklySummaryCalculatorTest {

    private val monday = LocalDate.of(2026, 10, 12)
    private val tuesday = monday.plusDays(1)
    private val wednesday = monday.plusDays(2)
    private val friday = monday.plusDays(4)
    private val sunday = monday.plusDays(6)
    private val lastSunday = monday.minusDays(1)

    private val upperLower = schedule(MONDAY to 1, TUESDAY to 2, THURSDAY to 1, FRIDAY to 2)

    private fun schedule(vararg days: Pair<DayOfWeek, Long>): List<ScheduleDay> {
        val byDay = days.toMap()
        return DayOfWeek.entries.map { day ->
            val templateId = byDay[day]
            ScheduleDay(day, templateId, templateId?.let { if (it == 1L) "Upper" else "Lower" })
        }
    }

    private fun calculate(
        sessions: List<SessionDay> = emptyList(),
        sets: List<RecordedSet> = emptyList(),
        today: LocalDate = TODAY,
        schedule: List<ScheduleDay> = upperLower,
    ): WeeklySummary = WeeklySummaryCalculator.calculate(today, schedule, sessions, sets)

    private fun WeeklySummary.statuses(): List<DayStatus> = days.map { it.status }

    @Test
    fun `lists the seven days from monday to sunday with the scheduled template`() {
        val summary = calculate()

        assertEquals((0L..6L).map { monday.plusDays(it) }, summary.days.map { it.date })
        assertEquals(WeekDay(monday, "Upper", MISSED), summary.days[0])
        assertEquals(WeekDay(wednesday, null, REST), summary.days[2])
    }

    @Test
    fun `past scheduled day without session is missed and today and later are pending`() {
        assertEquals(
            listOf(MISSED, MISSED, REST, PENDING, PENDING, REST, REST),
            calculate().statuses(),
        )
    }

    @Test
    fun `scheduled day with a session of its template is done, today included`() {
        val summary = calculate(listOf(SessionDay(monday, 1, null), SessionDay(TODAY, 1, null)))

        assertEquals(listOf(DONE, MISSED, REST, DONE, PENDING, REST, REST), summary.statuses())
    }

    @Test
    fun `made up day counts as done and the makeup session is not extra`() {
        val summary = calculate(listOf(SessionDay(monday, 1, null), SessionDay(wednesday, 2, makeupForDate = tuesday)))

        assertEquals(MADE_UP, summary.days[1].status)
        assertEquals(REST, summary.days[2].status)
        assertEquals(4, summary.plannedCount)
        assertEquals(2, summary.doneCount)
        assertEquals(0, summary.extraCount)
    }

    @Test
    fun `another template on a scheduled day without makeup link leaves the day missed and counts as extra`() {
        val summary = calculate(listOf(SessionDay(monday, 2, null)))

        assertEquals(MISSED, summary.days[0].status)
        assertEquals(1, summary.extraCount)
    }

    @Test
    fun `free workout without makeup link only counts as extra`() {
        val summary = calculate(listOf(SessionDay(wednesday, null, null)))

        assertEquals(REST, summary.days[2].status)
        assertEquals(0, summary.doneCount)
        assertEquals(1, summary.extraCount)
    }

    @Test
    fun `scheduled template on an unscheduled day without makeup link is extra`() {
        val summary = calculate(listOf(SessionDay(wednesday, 2, null)))

        assertEquals(MISSED, summary.days[1].status)
        assertEquals(1, summary.extraCount)
    }

    @Test
    fun `second session of the same template on the day is extra`() {
        val summary = calculate(listOf(SessionDay(monday, 1, null), SessionDay(monday, 1, null)))

        assertEquals(DONE, summary.days[0].status)
        assertEquals(1, summary.doneCount)
        assertEquals(1, summary.extraCount)
    }

    @Test
    fun `makeup of a day that is no longer scheduled is extra`() {
        val summary = calculate(listOf(SessionDay(wednesday, 2, makeupForDate = tuesday)), schedule = schedule(MONDAY to 1))

        assertEquals(REST, summary.days[1].status)
        assertEquals(1, summary.extraCount)
    }

    @Test
    fun `sessions outside the week are ignored`() {
        val summary = calculate(listOf(SessionDay(lastSunday, 1, null), SessionDay(lastSunday, null, null)))

        assertEquals(MISSED, summary.days[0].status)
        assertEquals(0, summary.extraCount)
    }

    @Test
    fun `on monday the week starts today and nothing is missed yet`() {
        val summary = calculate(today = monday)

        assertEquals(listOf(PENDING, PENDING, REST, PENDING, PENDING, REST, REST), summary.statuses())
    }

    @Test
    fun `on sunday the week still starts on the previous monday`() {
        val summary = calculate(listOf(SessionDay(friday, 2, null)), today = sunday)

        assertEquals(monday, summary.days.first().date)
        assertEquals(listOf(MISSED, MISSED, REST, MISSED, DONE, REST, REST), summary.statuses())
    }

    @Test
    fun `volume sums the completed sets of the week, extras included`() {
        val sets = listOf(
            recordedSet(1, 100_000, 5, date = lastSunday, sessionId = 1),
            recordedSet(2, 60_000, 10, date = monday, sessionId = 2),
            recordedSet(3, 62_500, 8, date = wednesday, sessionId = 3, exerciseId = 2),
        )

        val summary = calculate(listOf(SessionDay(monday, 1, null), SessionDay(wednesday, null, null)), sets)

        assertEquals(600_000L + 500_000L, summary.volume)
    }

    @Test
    fun `counts sets of the week that beat the whole history`() {
        val sets = listOf(
            recordedSet(1, 60_000, 10, date = lastSunday, sessionId = 1),
            recordedSet(2, 62_500, 8, date = lastSunday, sessionId = 1),
            recordedSet(3, 65_000, 6, date = monday, sessionId = 2),
            recordedSet(4, 60_000, 11, date = monday, sessionId = 2),
            recordedSet(5, 60_000, 11, date = wednesday, sessionId = 3),
        )

        assertEquals(2, calculate(sets = sets).recordCount)
    }

    @Test
    fun `first execution in the week is a reference, not a record`() {
        val sets = listOf(
            recordedSet(1, 60_000, 10, date = monday, sessionId = 2, exerciseId = 3),
            recordedSet(2, 40_000, 12, date = monday, sessionId = 2, exerciseId = 4),
        )

        assertEquals(0, calculate(sets = sets).recordCount)
    }
}
