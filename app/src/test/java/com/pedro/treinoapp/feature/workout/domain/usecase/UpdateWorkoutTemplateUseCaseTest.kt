package com.pedro.treinoapp.feature.workout.domain.usecase

import com.pedro.treinoapp.feature.workout.domain.FakeWorkoutTemplateRepository
import com.pedro.treinoapp.feature.workout.domain.model.RepRange
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutError
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutResult
import com.pedro.treinoapp.feature.workout.domain.template
import com.pedro.treinoapp.feature.workout.domain.templateExercise
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateWorkoutTemplateUseCaseTest {

    private val upper = template(1, "Upper", 10, 11)
    private val lower = template(2, "Lower", 20, 21)
    private val repository = FakeWorkoutTemplateRepository(listOf(upper, lower))
    private val update = UpdateWorkoutTemplateUseCase(repository, ValidateWorkoutTemplateUseCase(repository))

    @Test
    fun `name, order and targets are replaced`() = runTest {
        val exercises = listOf(templateExercise(11, sets = 4, reps = RepRange.fixed(6)), templateExercise(10))

        val result = update(1, "Upper A", exercises)

        assertEquals(WorkoutResult.Success(Unit), result)
        val saved = repository.getById(1)!!
        assertEquals("Upper A", saved.name)
        assertEquals(exercises, saved.exercises)
    }

    @Test
    fun `other templates are not touched`() = runTest {
        update(1, "Upper A", listOf(templateExercise(12)))

        assertEquals(lower, repository.getById(2))
    }

    @Test
    fun `cannot take the name of another template`() = runTest {
        val result = update(1, "lower", upper.exercises)

        assertEquals(WorkoutResult.Failure(WorkoutError.NameAlreadyExists), result)
        assertEquals(upper, repository.getById(1))
    }

    @Test
    fun `removing every exercise is rejected`() = runTest {
        assertEquals(WorkoutResult.Failure(WorkoutError.NoExercises), update(1, "Upper", emptyList()))
    }

    @Test
    fun `missing template returns not found`() = runTest {
        assertEquals(WorkoutResult.Failure(WorkoutError.NotFound), update(99, "X", upper.exercises))
    }
}
