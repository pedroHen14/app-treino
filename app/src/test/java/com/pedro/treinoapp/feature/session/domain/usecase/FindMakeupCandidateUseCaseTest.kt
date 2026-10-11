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
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.LocalDate

/** Hoje é quinta (15/10/2026); a semana começou na segunda, 12/10. */
class FindMakeupCandidateUseCaseTest {

    private val monday: LocalDate = TODAY.minusDays(3)
    private val tuesday: LocalDate = TODAY.minusDays(2)

    private val upper = 1L
    private val lower = 2L

    /** Upper na segunda e quinta, Lower na terça e sexta, como na rotina do app. */
    private val upperLower = mapOf<DayOfWeek, Long?>(MONDAY to upper, TUESDAY to lower, THURSDAY to upper, FRIDAY to lower)

    private fun find(schedule: Map<DayOfWeek, Long?>, sessions: FakeSessionRepository = FakeSessionRepository()) =
        FindMakeupCandidateUseCase(FakeWeeklyScheduleRepository(schedule), sessions, fixedClock)

    @Test
    fun `pending earlier day with the template is suggested`() = runTest {
        assertEquals(tuesday, find(upperLower)(lower))
    }

    @Test
    fun `no question when the template is scheduled for today`() = runTest {
        assertNull(find(upperLower)(upper))
    }

    @Test
    fun `rest day today also asks`() = runTest {
        assertEquals(monday, find(mapOf(MONDAY to upper))(upper))
    }

    @Test
    fun `day with a session of its template is not pending`() = runTest {
        val sessions = FakeSessionRepository(listOf(session(1, tuesday, templateId = lower)))

        assertNull(find(upperLower, sessions)(lower))
    }

    @Test
    fun `day already made up is not pending`() = runTest {
        val sessions = FakeSessionRepository(listOf(session(1, TODAY.minusDays(1), templateId = lower, makeupForDate = tuesday)))

        assertNull(find(upperLower, sessions)(lower))
    }

    @Test
    fun `day where another workout was done stays pending`() = runTest {
        val sessions = FakeSessionRepository(listOf(session(1, tuesday, templateId = upper)))

        assertEquals(tuesday, find(upperLower, sessions)(lower))
    }

    @Test
    fun `makeup done on a day does not fulfill that day`() = runTest {
        val schedule = mapOf<DayOfWeek, Long?>(MONDAY to upper, TUESDAY to upper)
        val sessions = FakeSessionRepository(listOf(session(1, tuesday, templateId = upper, makeupForDate = monday)))

        assertEquals(tuesday, find(schedule, sessions)(upper))
    }

    @Test
    fun `oldest pending day is suggested`() = runTest {
        val schedule = mapOf<DayOfWeek, Long?>(MONDAY to lower, TUESDAY to lower)

        assertEquals(monday, find(schedule)(lower))
    }

    @Test
    fun `days from previous weeks and later days are never suggested`() = runTest {
        val schedule = mapOf<DayOfWeek, Long?>(FRIDAY to lower, SUNDAY to lower)

        assertNull(find(schedule)(lower))
    }

    @Test
    fun `template not in the schedule has nothing to make up`() = runTest {
        assertNull(find(upperLower)(3))
    }
}
