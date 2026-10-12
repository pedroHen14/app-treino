package com.pedro.treinoapp.feature.exercise.domain.repository

import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseResult
import com.pedro.treinoapp.feature.exercise.domain.model.WgerExercise

interface WgerExerciseRepository {
    /** Falha com `NoConnection` ou `ServiceUnavailable`. */
    suspend fun search(term: String): ExerciseResult<List<WgerExercise>>
}
