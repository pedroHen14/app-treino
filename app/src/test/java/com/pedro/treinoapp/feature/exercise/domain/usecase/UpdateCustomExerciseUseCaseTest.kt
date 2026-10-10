package com.pedro.treinoapp.feature.exercise.domain.usecase

import com.pedro.treinoapp.feature.exercise.domain.FakeExerciseRepository
import com.pedro.treinoapp.feature.exercise.domain.model.Exercise
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseError
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseResult
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateCustomExerciseUseCaseTest {

    private val seed = Exercise(id = 1, name = "Supino reto", muscleGroup = MuscleGroup.CHEST, isCustom = false)
    private val custom = Exercise(id = 2, name = "Rosca martelo", muscleGroup = MuscleGroup.BICEPS, isCustom = true)
    private val repository = FakeExerciseRepository(listOf(seed, custom))
    private val update = UpdateCustomExerciseUseCase(repository, ValidateExerciseNameUseCase(repository))

    @Test
    fun `updates name and muscle group of custom exercise`() = runTest {
        assertEquals(ExerciseResult.Success(Unit), update(2, " Rosca  alternada ", MuscleGroup.FOREARMS))

        assertEquals(
            custom.copy(name = "Rosca alternada", muscleGroup = MuscleGroup.FOREARMS),
            repository.getById(2),
        )
    }

    @Test
    fun `custom exercise can change only the case of its own name`() = runTest {
        assertEquals(ExerciseResult.Success(Unit), update(2, "Rosca Martelo", MuscleGroup.BICEPS))
        assertEquals("Rosca Martelo", repository.getById(2)?.name)
    }

    @Test
    fun `cannot rename to an existing name`() = runTest {
        assertEquals(
            ExerciseResult.Failure(ExerciseError.NameAlreadyExists),
            update(2, "SUPINO RETO", MuscleGroup.BICEPS),
        )
        assertEquals(custom, repository.getById(2))
    }

    @Test
    fun `cannot rename to blank`() = runTest {
        assertEquals(ExerciseResult.Failure(ExerciseError.NameBlank), update(2, "", MuscleGroup.BICEPS))
    }

    @Test
    fun `seed exercise cannot be edited`() = runTest {
        assertEquals(ExerciseResult.Failure(ExerciseError.NotCustom), update(1, "Supino", MuscleGroup.CHEST))
        assertEquals(seed, repository.getById(1))
    }

    @Test
    fun `missing exercise returns not found`() = runTest {
        assertEquals(ExerciseResult.Failure(ExerciseError.NotFound), update(99, "Novo", MuscleGroup.CHEST))
    }
}
