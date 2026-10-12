package com.pedro.treinoapp.feature.exercise.domain

import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseResult
import com.pedro.treinoapp.feature.exercise.domain.model.WgerExercise
import com.pedro.treinoapp.feature.exercise.domain.repository.WgerExerciseRepository

class FakeWgerExerciseRepository(
    var result: ExerciseResult<List<WgerExercise>> = ExerciseResult.Success(emptyList()),
) : WgerExerciseRepository {

    val searchedTerms = mutableListOf<String>()

    override suspend fun search(term: String): ExerciseResult<List<WgerExercise>> {
        searchedTerms += term
        return result
    }
}
