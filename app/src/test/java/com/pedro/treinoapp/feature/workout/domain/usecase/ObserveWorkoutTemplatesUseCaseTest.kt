package com.pedro.treinoapp.feature.workout.domain.usecase

import com.pedro.treinoapp.feature.workout.domain.FakeWeeklyScheduleRepository
import com.pedro.treinoapp.feature.workout.domain.FakeWorkoutTemplateRepository
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutTemplateSummary
import com.pedro.treinoapp.feature.workout.domain.template
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY

class ObserveWorkoutTemplatesUseCaseTest {

    @Test
    fun `templates come sorted by name with their scheduled days`() = runTest {
        val templates = FakeWorkoutTemplateRepository(
            listOf(template(1, "Upper", 10, 11), template(2, "Lower", 20), template(3, "Ênfase braço", 30)),
        )
        val schedule = FakeWeeklyScheduleRepository(
            mapOf(THURSDAY to 1L, MONDAY to 1L, TUESDAY to 2L, FRIDAY to null),
        )

        val result = ObserveWorkoutTemplatesUseCase(templates, schedule)().first()

        assertEquals(
            listOf(
                WorkoutTemplateSummary(3, "Ênfase braço", 1, emptyList()),
                WorkoutTemplateSummary(2, "Lower", 1, listOf(TUESDAY)),
                WorkoutTemplateSummary(1, "Upper", 2, listOf(MONDAY, THURSDAY)),
            ),
            result,
        )
    }
}
