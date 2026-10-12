package com.pedro.treinoapp.feature.exercise.domain.usecase

import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseResult
import com.pedro.treinoapp.feature.exercise.domain.model.WgerExercise
import com.pedro.treinoapp.feature.exercise.domain.repository.WgerExerciseRepository
import javax.inject.Inject

/** Termos com menos de [MIN_TERM_LENGTH] letras não vão para a API: a busca aproximada traria ruído. */
class SearchWgerExercisesUseCase @Inject constructor(
    private val repository: WgerExerciseRepository,
) {
    suspend operator fun invoke(term: String): ExerciseResult<List<WgerExercise>> {
        val trimmed = term.trim()
        if (trimmed.length < MIN_TERM_LENGTH) return ExerciseResult.Success(emptyList())
        return repository.search(trimmed)
    }

    companion object {
        const val MIN_TERM_LENGTH = 2
    }
}
