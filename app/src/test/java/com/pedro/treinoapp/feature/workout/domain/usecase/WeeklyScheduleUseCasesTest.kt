package com.pedro.treinoapp.feature.workout.domain.usecase

import com.pedro.treinoapp.feature.workout.domain.FakeWeeklyScheduleRepository
import com.pedro.treinoapp.feature.workout.domain.FakeWorkoutTemplateRepository
import com.pedro.treinoapp.feature.workout.domain.model.ScheduleDay
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutError
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutResult
import com.pedro.treinoapp.feature.workout.domain.template
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY

class WeeklyScheduleUseCasesTest {

    private val templates = FakeWorkoutTemplateRepository(listOf(template(1, "Upper", 10), template(2, "Lower", 20)))
    private val schedule = FakeWeeklyScheduleRepository()
    private val observe = ObserveWeeklyScheduleUseCase(schedule, templates)
    private val setDay = SetScheduleDayUseCase(schedule, templates)

    @Test
    fun `empty schedule has seven rest days from monday to sunday`() = runTest {
        val days = observe().first()

        assertEquals(DayOfWeek.entries.toList(), days.map { it.dayOfWeek })
        assertTrue(days.all { it.isRest })
    }

    @Test
    fun `same template can be on several days`() = runTest {
        setDay(MONDAY, 1)
        setDay(TUESDAY, 2)
        setDay(THURSDAY, 1)
        setDay(FRIDAY, 2)

        assertEquals(
            listOf(
                ScheduleDay(MONDAY, 1, "Upper"),
                ScheduleDay(TUESDAY, 2, "Lower"),
                ScheduleDay(WEDNESDAY),
                ScheduleDay(THURSDAY, 1, "Upper"),
                ScheduleDay(FRIDAY, 2, "Lower"),
                ScheduleDay(SATURDAY),
                ScheduleDay(SUNDAY),
            ),
            observe().first(),
        )
    }

    @Test
    fun `day can be changed back to rest`() = runTest {
        setDay(MONDAY, 1)

        assertEquals(WorkoutResult.Success(Unit), setDay(MONDAY, null))
        assertTrue(observe().first().first().isRest)
    }

    @Test
    fun `missing template is rejected and the day is kept`() = runTest {
        setDay(MONDAY, 1)

        assertEquals(WorkoutResult.Failure(WorkoutError.NotFound), setDay(MONDAY, 99))
        assertEquals(ScheduleDay(MONDAY, 1, "Upper"), observe().first().first())
    }
}
