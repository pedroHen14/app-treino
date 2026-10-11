package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.FakeSessionRepository
import com.pedro.treinoapp.feature.session.domain.TODAY
import com.pedro.treinoapp.feature.session.domain.fixedClock
import com.pedro.treinoapp.feature.session.domain.model.TodayPlan
import com.pedro.treinoapp.feature.session.domain.session
import com.pedro.treinoapp.feature.workout.domain.FakeWeeklyScheduleRepository
import com.pedro.treinoapp.feature.workout.domain.FakeWorkoutTemplateRepository
import com.pedro.treinoapp.feature.workout.domain.template
import com.pedro.treinoapp.feature.workout.domain.usecase.ObserveWeeklyScheduleUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY

/** Hoje é quinta. */
class ObserveTodayPlanUseCaseTest {

    private val templates = FakeWorkoutTemplateRepository(listOf(template(1, "Upper", 10), template(2, "Lower", 20)))

    private fun observe(schedule: FakeWeeklyScheduleRepository, sessions: FakeSessionRepository = FakeSessionRepository()) =
        ObserveTodayPlanUseCase(ObserveWeeklyScheduleUseCase(schedule, templates), sessions, fixedClock)

    @Test
    fun `shows the template scheduled for today`() = runTest {
        val plan = observe(FakeWeeklyScheduleRepository(mapOf(MONDAY to 2, THURSDAY to 1)))().first()

        assertEquals(TodayPlan(TODAY, 1, "Upper", isScheduledDone = false, null, null), plan)
    }

    @Test
    fun `day without template is rest`() = runTest {
        val plan = observe(FakeWeeklyScheduleRepository(mapOf(MONDAY to 1)))().first()

        assertTrue(plan.isRestDay)
    }

    @Test
    fun `scheduled workout is done after a finished session of it today`() = runTest {
        val sessions = FakeSessionRepository(listOf(session(5, TODAY, templateId = 1)))

        val plan = observe(FakeWeeklyScheduleRepository(mapOf(THURSDAY to 1)), sessions)().first()

        assertTrue(plan.isScheduledDone)
    }

    @Test
    fun `active session is reported to be resumed`() = runTest {
        val sessions = FakeSessionRepository(listOf(session(5, TODAY, templateId = 2, finished = false)))

        val plan = observe(FakeWeeklyScheduleRepository(mapOf(THURSDAY to 1)), sessions)().first()

        assertEquals(5L, plan.activeSessionId)
        assertEquals("Modelo 2", plan.activeSessionName)
        assertEquals(false, plan.isScheduledDone)
    }
}
