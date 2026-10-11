package com.pedro.treinoapp.feature.workout.domain.usecase

import com.pedro.treinoapp.feature.workout.domain.FakeWorkoutTemplateRepository
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutError
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutResult
import com.pedro.treinoapp.feature.workout.domain.templateExercise
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateWorkoutTemplateUseCaseTest {

    private val repository = FakeWorkoutTemplateRepository()
    private val create = CreateWorkoutTemplateUseCase(repository, ValidateWorkoutTemplateUseCase(repository))

    @Test
    fun `valid template is saved with normalized name and given order`() = runTest {
        val exercises = listOf(templateExercise(3), templateExercise(1), templateExercise(2))

        val result = create("  Upper ", exercises)

        val id = (result as WorkoutResult.Success).value
        val saved = repository.getById(id)!!
        assertEquals("Upper", saved.name)
        assertEquals(listOf(3L, 1L, 2L), saved.exercises.map { it.exerciseId })
    }

    @Test
    fun `invalid template is not saved`() = runTest {
        val result = create("Upper", emptyList())

        assertEquals(WorkoutResult.Failure(WorkoutError.NoExercises), result)
        assertTrue(repository.current.isEmpty())
    }
}
