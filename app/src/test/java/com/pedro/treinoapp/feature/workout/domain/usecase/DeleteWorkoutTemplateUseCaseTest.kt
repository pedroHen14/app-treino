package com.pedro.treinoapp.feature.workout.domain.usecase

import com.pedro.treinoapp.feature.workout.domain.FakeWeeklyScheduleRepository
import com.pedro.treinoapp.feature.workout.domain.FakeWorkoutTemplateRepository
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutError
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutResult
import com.pedro.treinoapp.feature.workout.domain.template
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY

class DeleteWorkoutTemplateUseCaseTest {

    private val templates = FakeWorkoutTemplateRepository(listOf(template(1, "Upper", 10), template(2, "Lower", 20)))
    private val schedule = FakeWeeklyScheduleRepository(
        mapOf(MONDAY to 1L, TUESDAY to 2L, WEDNESDAY to null, THURSDAY to 1L, FRIDAY to 2L),
    )
    private val delete = DeleteWorkoutTemplateUseCase(templates, schedule)
    private val scheduledDays = GetTemplateScheduledDaysUseCase(schedule)

    @Test
    fun `scheduled days of the template are listed for confirmation`() = runTest {
        assertEquals(listOf(MONDAY, THURSDAY), scheduledDays(1))
    }

    @Test
    fun `deleting turns its scheduled days into rest`() = runTest {
        assertEquals(WorkoutResult.Success(Unit), delete(1))

        assertNull(templates.getById(1))
        assertEquals(
            mapOf(MONDAY to null, TUESDAY to 2L, WEDNESDAY to null, THURSDAY to null, FRIDAY to 2L),
            schedule.current,
        )
    }

    @Test
    fun `missing template returns not found and keeps the schedule`() = runTest {
        val before = schedule.current

        assertEquals(WorkoutResult.Failure(WorkoutError.NotFound), delete(99))
        assertEquals(before, schedule.current)
    }
}
