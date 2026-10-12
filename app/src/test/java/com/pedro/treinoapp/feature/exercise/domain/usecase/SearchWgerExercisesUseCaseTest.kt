package com.pedro.treinoapp.feature.exercise.domain.usecase

import com.pedro.treinoapp.feature.exercise.domain.FakeWgerExerciseRepository
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseError
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseResult
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import com.pedro.treinoapp.feature.exercise.domain.model.WgerExercise
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchWgerExercisesUseCaseTest {

    private val repository = FakeWgerExerciseRepository()
    private val search = SearchWgerExercisesUseCase(repository)

    @Test
    fun `short or blank term returns nothing without calling the api`() = runTest {
        assertEquals(ExerciseResult.Success(emptyList<WgerExercise>()), search(" "))
        assertEquals(ExerciseResult.Success(emptyList<WgerExercise>()), search(" s "))
        assertEquals(emptyList<String>(), repository.searchedTerms)
    }

    @Test
    fun `searches with trimmed term`() = runTest {
        val found = listOf(WgerExercise(75, "Supino com halteres", true, null, MuscleGroup.CHEST))
        repository.result = ExerciseResult.Success(found)

        assertEquals(ExerciseResult.Success(found), search("  supino "))
        assertEquals(listOf("supino"), repository.searchedTerms)
    }

    @Test
    fun `passes connection errors through`() = runTest {
        repository.result = ExerciseResult.Failure(ExerciseError.NoConnection)
        assertEquals(ExerciseResult.Failure(ExerciseError.NoConnection), search("supino"))

        repository.result = ExerciseResult.Failure(ExerciseError.ServiceUnavailable)
        assertEquals(ExerciseResult.Failure(ExerciseError.ServiceUnavailable), search("supino"))
    }
}
