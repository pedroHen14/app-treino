package com.pedro.treinoapp.feature.workout.domain.usecase

import com.pedro.treinoapp.feature.workout.domain.FakeWeeklyScheduleRepository
import com.pedro.treinoapp.feature.workout.domain.FakeWorkoutTemplateRepository
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutError
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutResult
import com.pedro.treinoapp.feature.workout.domain.template
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

class DuplicateWorkoutTemplateUseCaseTest {

    private val upper = template(1, "Upper", 10, 11, 12)
    private val repository = FakeWorkoutTemplateRepository(listOf(upper))
    private val schedule = FakeWeeklyScheduleRepository(mapOf(DayOfWeek.MONDAY to 1L))
    private val duplicate = DuplicateWorkoutTemplateUseCase(repository)

    private val copyName = { original: String, n: Int ->
        if (n == 1) "$original (cópia)" else "$original (cópia $n)"
    }

    @Test
    fun `copy keeps exercises in the same order`() = runTest {
        val id = (duplicate(1, copyName) as WorkoutResult.Success).value

        val copy = repository.getById(id)!!
        assertEquals("Upper (cópia)", copy.name)
        assertEquals(upper.exercises, copy.exercises)
    }

    @Test
    fun `next free copy number is used`() = runTest {
        repository.insert(template(0, "UPPER (CÓPIA)", 10))

        val id = (duplicate(1, copyName) as WorkoutResult.Success).value

        assertEquals("Upper (cópia 2)", repository.getById(id)!!.name)
    }

    @Test
    fun `copy is not added to the schedule`() = runTest {
        val id = (duplicate(1, copyName) as WorkoutResult.Success).value

        assertEquals(emptyList<DayOfWeek>(), schedule.getDaysFor(id))
        assertEquals(mapOf(DayOfWeek.MONDAY to 1L), schedule.current)
    }

    @Test
    fun `missing template returns not found`() = runTest {
        assertEquals(WorkoutResult.Failure(WorkoutError.NotFound), duplicate(99, copyName))
    }
}
