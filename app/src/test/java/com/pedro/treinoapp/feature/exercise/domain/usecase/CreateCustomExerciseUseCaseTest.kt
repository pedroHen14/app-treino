package com.pedro.treinoapp.feature.exercise.domain.usecase

import com.pedro.treinoapp.feature.exercise.domain.FakeExerciseRepository
import com.pedro.treinoapp.feature.exercise.domain.model.Exercise
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseError
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseResult
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CreateCustomExerciseUseCaseTest {

    private val repository = FakeExerciseRepository(
        listOf(Exercise(id = 1, name = "Supino reto", muscleGroup = MuscleGroup.CHEST, isCustom = false)),
    )
    private val create = CreateCustomExerciseUseCase(repository, ValidateExerciseNameUseCase(repository))

    @Test
    fun `creates custom, active exercise with normalized name`() = runTest {
        val result = create("  Rosca   martelo ", MuscleGroup.BICEPS)

        val id = (result as ExerciseResult.Success).value
        assertEquals(
            Exercise(id = id, name = "Rosca martelo", muscleGroup = MuscleGroup.BICEPS, isCustom = true),
            repository.getById(id),
        )
    }

    @Test
    fun `does not create exercise with blank name`() = runTest {
        assertEquals(ExerciseResult.Failure(ExerciseError.NameBlank), create(" ", MuscleGroup.BICEPS))
        assertEquals(1, repository.current.size)
    }

    @Test
    fun `does not create exercise with existing name`() = runTest {
        assertEquals(
            ExerciseResult.Failure(ExerciseError.NameAlreadyExists),
            create("supino RETO", MuscleGroup.CHEST),
        )
        assertEquals(1, repository.current.size)
    }
}
