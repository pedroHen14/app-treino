package com.pedro.treinoapp.feature.exercise.domain.usecase

import com.pedro.treinoapp.feature.exercise.domain.FakeExerciseRepository
import com.pedro.treinoapp.feature.exercise.domain.model.Exercise
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseError
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseResult
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ArchiveExerciseUseCaseTest {

    private val repository = FakeExerciseRepository(
        listOf(
            Exercise(id = 1, name = "Supino reto", muscleGroup = MuscleGroup.CHEST, isCustom = false),
            Exercise(id = 2, name = "Rosca martelo", muscleGroup = MuscleGroup.BICEPS, isCustom = true),
        ),
    ).apply { usedInSession += 2 }
    private val archive = ArchiveExerciseUseCase(repository)
    private val unarchive = UnarchiveExerciseUseCase(repository)

    @Test
    fun `seed exercise can be archived`() = runTest {
        assertEquals(ExerciseResult.Success(Unit), archive(1))
        assertEquals(true, repository.getById(1)?.isArchived)
    }

    @Test
    fun `exercise used in a session can be archived and stays stored`() = runTest {
        assertEquals(ExerciseResult.Success(Unit), archive(2))
        assertEquals(true, repository.getById(2)?.isArchived)
    }

    @Test
    fun `archived exercise can be unarchived`() = runTest {
        archive(2)

        assertEquals(ExerciseResult.Success(Unit), unarchive(2))
        assertEquals(false, repository.getById(2)?.isArchived)
    }

    @Test
    fun `archiving twice keeps exercise archived`() = runTest {
        archive(1)
        assertEquals(ExerciseResult.Success(Unit), archive(1))
        assertEquals(true, repository.getById(1)?.isArchived)
    }

    @Test
    fun `missing exercise returns not found`() = runTest {
        assertEquals(ExerciseResult.Failure(ExerciseError.NotFound), archive(99))
        assertEquals(ExerciseResult.Failure(ExerciseError.NotFound), unarchive(99))
    }
}
