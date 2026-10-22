package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.FakeSessionRepository
import com.pedro.treinoapp.feature.session.domain.TODAY
import com.pedro.treinoapp.feature.session.domain.fixedClock
import com.pedro.treinoapp.feature.session.domain.session
import com.pedro.treinoapp.feature.workout.domain.FakeWeeklyScheduleRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.LocalDate

/** Hoje é quinta (15/10/2026); a semana vai até domingo, 18/10. */
class FindAdvanceCandidateUseCaseTest {

    private val friday: LocalDate = TODAY.plusDays(1)
    private val saturday: LocalDate = TODAY.plusDays(2)

    private val upper = 1L
    private val lower = 2L

    /** Upper na segunda e quinta, Lower na terça e sexta, como na rotina do app. */
    private val upperLower = mapOf<DayOfWeek, Long?>(MONDAY to upper, TUESDAY to lower, THURSDAY to upper, FRIDAY to lower)

    private fun find(schedule: Map<DayOfWeek, Long?>, sessions: FakeSessionRepository = FakeSessionRepository()) =
        FindAdvanceCandidateUseCase(FakeWeeklyScheduleRepository(schedule), sessions, fixedClock)

    @Test
    fun `later day of the week with the template is suggested`() = runTest {
        assertEquals(friday, find(upperLower)(lower))
    }

    @Test
    fun `no question when the template is scheduled for today`() = runTest {
        assertNull(find(upperLower)(upper))
    }

    @Test
    fun `rest day today also asks`() = runTest {
        assertEquals(friday, find(mapOf(FRIDAY to upper))(upper))
    }

    @Test
    fun `nearest later day is suggested`() = runTest {
        assertEquals(friday, find(mapOf(FRIDAY to lower, SATURDAY to lower))(lower))
    }

    @Test
    fun `day already advanced is skipped`() = runTest {
        val schedule = mapOf<DayOfWeek, Long?>(FRIDAY to lower, SATURDAY to lower)
        val sessions = FakeSessionRepository(listOf(session(1, TODAY.minusDays(1), templateId = lower, makeupForDate = friday)))

        assertEquals(saturday, find(schedule, sessions)(lower))
    }

    @Test
    fun `earlier days and days of the next week are never suggested`() = runTest {
        assertNull(find(mapOf(MONDAY to lower, TUESDAY to lower))(lower))
    }

    @Test
    fun `template not in the schedule has nothing to advance`() = runTest {
        assertNull(find(upperLower)(3))
    }
}
