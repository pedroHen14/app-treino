package com.pedro.treinoapp.feature.exercise.domain.usecase

import com.pedro.treinoapp.core.text.toSearchKey
import com.pedro.treinoapp.feature.exercise.domain.model.Exercise
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseFilter
import com.pedro.treinoapp.feature.exercise.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

/**
 * Lista filtrada e em ordem alfabética do português. O filtro roda em memória:
 * o catálogo é pequeno e assim a busca ignora acentos, o que o LIKE do SQLite não faz.
 */
class ObserveExercisesUseCase @Inject constructor(
    private val repository: ExerciseRepository,
) {
    private val collator = Collator.getInstance(Locale.forLanguageTag("pt-BR"))

    operator fun invoke(filter: Flow<ExerciseFilter>): Flow<List<Exercise>> =
        combine(repository.observeAll(), filter) { exercises, current ->
            val query = current.query.toSearchKey()
            exercises
                .filter { current.includeArchived || !it.isArchived }
                .filter { current.muscleGroup == null || it.muscleGroup == current.muscleGroup }
                .filter { query.isEmpty() || it.name.toSearchKey().contains(query) }
                .sortedWith(compareBy(collator) { it.name })
        }
}
