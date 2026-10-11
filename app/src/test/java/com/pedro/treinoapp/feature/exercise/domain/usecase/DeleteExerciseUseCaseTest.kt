package com.pedro.treinoapp.feature.exercise.domain.usecase

import com.pedro.treinoapp.feature.exercise.domain.FakeExerciseRepository
import com.pedro.treinoapp.feature.exercise.domain.model.Exercise
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseError
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseResult
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DeleteExerciseUseCaseTest {

    private val repository = FakeExerciseRepository(
        listOf(
            Exercise(id = 1, name = "Supino reto", muscleGroup = MuscleGroup.CHEST, isCustom = false),
            Exercise(id = 2, name = "Rosca martelo", muscleGroup = MuscleGroup.BICEPS, isCustom = true),
            Exercise(id = 3, name = "Remada baixa", muscleGroup = MuscleGroup.BACK, isCustom = true),
            Exercise(id = 4, name = "Stiff", muscleGroup = MuscleGroup.HAMSTRINGS, isCustom = true),
        ),
    ).apply {
        usedInSession += 3
        usedInTemplate += 4
    }
    private val delete = DeleteExerciseUseCase(repository)

    @Test
    fun `unused custom exercise is deleted`() = runTest {
        assertEquals(ExerciseResult.Success(Unit), delete(2))
        assertNull(repository.getById(2))
    }

    @Test
    fun `exercise used in a session cannot be deleted`() = runTest {
        assertEquals(ExerciseResult.Failure(ExerciseError.UsedInSession), delete(3))
        assertNotNull(repository.getById(3))
    }

    @Test
    fun `exercise used in a template cannot be deleted`() = runTest {
        assertEquals(ExerciseResult.Failure(ExerciseError.UsedInTemplate), delete(4))
        assertNotNull(repository.getById(4))
    }

    @Test
    fun `session usage is reported before template usage`() = runTest {
        repository.usedInTemplate += 3
        assertEquals(ExerciseResult.Failure(ExerciseError.UsedInSession), delete(3))
    }

    @Test
    fun `seed exercise cannot be deleted`() = runTest {
        assertEquals(ExerciseResult.Failure(ExerciseError.NotCustom), delete(1))
        assertNotNull(repository.getById(1))
    }

    @Test
    fun `missing exercise returns not found`() = runTest {
        assertEquals(ExerciseResult.Failure(ExerciseError.NotFound), delete(99))
    }
}
