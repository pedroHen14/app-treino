package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.model.WorkoutSet
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import com.pedro.treinoapp.feature.workout.domain.model.TemplateExercise
import javax.inject.Inject

/**
 * Séries novas de um exercício, preenchidas com carga e repetições da última execução dele:
 * a série N copia a série N anterior, ou a última, se da outra vez foram menos séries.
 * Sem execução anterior, as séries vêm vazias e a tela mostra o alvo como dica.
 */
class BuildPrefilledSetsUseCase @Inject constructor(
    private val repository: SessionRepository,
) {
    /** @param count quantidade de séries; nulo repete a quantidade da última execução (ou o padrão). */
    suspend operator fun invoke(exerciseId: Long, count: Int? = null): List<WorkoutSet> {
        val last = repository.getLastCompletedSets(exerciseId).filter { it.isFilled }
        val size = count ?: last.size.takeIf { it > 0 } ?: TemplateExercise.DEFAULT_SETS
        if (last.isEmpty()) return List(size) { WorkoutSet() }
        return List(size) { index ->
            val source = last.getOrElse(index) { last.last() }
            WorkoutSet(weightGrams = source.weightGrams, reps = source.reps)
        }
    }
}
