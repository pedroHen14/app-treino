package com.pedro.treinoapp.feature.exercise.domain.usecase

import com.pedro.treinoapp.feature.exercise.domain.FakeExerciseRepository
import com.pedro.treinoapp.feature.exercise.domain.model.Exercise
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseError
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseResult
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateExerciseNameUseCaseTest {

    private val repository = FakeExerciseRepository(
        listOf(
            Exercise(id = 1, name = "Supino reto", muscleGroup = MuscleGroup.CHEST, isCustom = false),
            Exercise(id = 2, name = "Elevação lateral", muscleGroup = MuscleGroup.SHOULDERS, isCustom = false),
            Exercise(
                id = 3,
                name = "Remada antiga",
                muscleGroup = MuscleGroup.BACK,
                isCustom = true,
                isArchived = true,
            ),
        ),
    )
    private val validate = ValidateExerciseNameUseCase(repository)

    @Test
    fun `empty name is rejected`() = runTest {
        assertEquals(ExerciseResult.Failure(ExerciseError.NameBlank), validate(""))
    }

    @Test
    fun `name with only spaces is rejected`() = runTest {
        assertEquals(ExerciseResult.Failure(ExerciseError.NameBlank), validate("   \t "))
    }

    @Test
    fun `name is trimmed and inner spaces collapsed`() = runTest {
        assertEquals(ExerciseResult.Success("Rosca direta"), validate("  Rosca   direta "))
    }

    @Test
    fun `duplicate with different case is rejected`() = runTest {
        assertEquals(ExerciseResult.Failure(ExerciseError.NameAlreadyExists), validate("SUPINO RETO"))
    }

    @Test
    fun `duplicate with accented letters in different case is rejected`() = runTest {
        assertEquals(ExerciseResult.Failure(ExerciseError.NameAlreadyExists), validate("ELEVAÇÃO LATERAL"))
    }

    @Test
    fun `duplicate with extra spaces is rejected`() = runTest {
        assertEquals(ExerciseResult.Failure(ExerciseError.NameAlreadyExists), validate(" supino  reto "))
    }

    @Test
    fun `name of archived exercise is still taken`() = runTest {
        assertEquals(ExerciseResult.Failure(ExerciseError.NameAlreadyExists), validate("remada antiga"))
    }

    @Test
    fun `same name without accent is a different exercise`() = runTest {
        assertEquals(ExerciseResult.Success("Elevacao lateral"), validate("Elevacao lateral"))
    }

    @Test
    fun `exercise can keep its own name when edited`() = runTest {
        assertEquals(ExerciseResult.Success("Supino Reto"), validate("Supino Reto", excludingId = 1))
    }
}
