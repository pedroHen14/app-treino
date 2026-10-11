package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.model.NewSessionExercise
import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * Inclui exercícios no fim da sessão, na ordem recebida; os que já estão nela são ignorados.
 * Sem alvo de modelo, cada um recebe as séries da última execução, ou o padrão de séries vazias.
 */
class AddSessionExercisesUseCase @Inject constructor(
    private val repository: SessionRepository,
    private val buildPrefilledSets: BuildPrefilledSetsUseCase,
) {
    suspend operator fun invoke(sessionId: Long, exerciseIds: List<Long>): SessionResult<Unit> {
        val session = repository.getEditable(sessionId) ?: return SessionResult.Failure(SessionError.NotFound)
        val present = session.exercises.map { it.exerciseId }.toSet()
        exerciseIds.distinct().filter { it !in present }.forEach { exerciseId ->
            repository.addExercise(sessionId, NewSessionExercise(exerciseId, sets = buildPrefilledSets(exerciseId)))
        }
        return SessionResult.Success(Unit)
    }
}
