package com.pedro.treinoapp.feature.weeklysummary.domain.usecase

import com.pedro.treinoapp.feature.history.domain.FakeHistoryRepository
import com.pedro.treinoapp.feature.history.domain.finishedSession
import com.pedro.treinoapp.feature.history.domain.recordedSet
import com.pedro.treinoapp.feature.session.domain.FakeSessionRepository
import com.pedro.treinoapp.feature.session.domain.TODAY
import com.pedro.treinoapp.feature.session.domain.fixedClock
import com.pedro.treinoapp.feature.session.domain.session
import com.pedro.treinoapp.feature.weeklysummary.domain.model.DayStatus.DONE
import com.pedro.treinoapp.feature.weeklysummary.domain.model.DayStatus.MISSED
import com.pedro.treinoapp.feature.weeklysummary.domain.model.DayStatus.PENDING
import com.pedro.treinoapp.feature.weeklysummary.domain.model.DayStatus.REST
import com.pedro.treinoapp.feature.workout.domain.FakeWeeklyScheduleRepository
import com.pedro.treinoapp.feature.workout.domain.FakeWorkoutTemplateRepository
import com.pedro.treinoapp.feature.workout.domain.template
import com.pedro.treinoapp.feature.workout.domain.usecase.ObserveWeeklyScheduleUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY

/** Hoje é quinta, 15/10/2026; a semana começou na segunda, 12/10. */
class ObserveWeeklySummaryUseCaseTest {

    private val monday = TODAY.minusDays(3)
    private val templates = FakeWorkoutTemplateRepository(listOf(template(1, "Upper", 10), template(2, "Lower", 20)))
    private val schedule = FakeWeeklyScheduleRepository(mapOf(MONDAY to 1, TUESDAY to 2, THURSDAY to 1))

    private fun observe(sessions: FakeSessionRepository, history: FakeHistoryRepository = FakeHistoryRepository()) =
        ObserveWeeklySummaryUseCase(ObserveWeeklyScheduleUseCase(schedule, templates), sessions, history, fixedClock)

    @Test
    fun `combines schedule and finished sessions of the current week`() = runTest {
        val sessions = FakeSessionRepository(
            listOf(
                session(1, monday, templateId = 1),
                session(2, monday.minusDays(7), templateId = 2),
                session(3, TODAY, templateId = 1, finished = false),
            ),
        )

        val summary = observe(sessions)().first()

        assertEquals(listOf(DONE, MISSED, REST, PENDING, REST, REST, REST), summary.days.map { it.status })
        assertEquals("Upper", summary.days[0].templateName)
        assertEquals(1, summary.doneCount)
        assertEquals(3, summary.plannedCount)
        assertEquals(0, summary.extraCount)
    }

    @Test
    fun `volume and records come from the completed sets history`() = runTest {
        val lastWeek = monday.minusDays(7)
        val history = FakeHistoryRepository(
            sessions = listOf(finishedSession(1, lastWeek), finishedSession(2, monday)),
            sets = listOf(
                recordedSet(1, 60_000, 10, date = lastWeek, sessionId = 1),
                recordedSet(2, 60_000, 12, date = monday, sessionId = 2),
                recordedSet(3, 50_000, 10, date = monday, sessionId = 2),
            ),
        )

        val summary = observe(FakeSessionRepository(), history)().first()

        assertEquals(720_000L + 500_000L, summary.volume)
        assertEquals(1, summary.recordCount)
    }
}
