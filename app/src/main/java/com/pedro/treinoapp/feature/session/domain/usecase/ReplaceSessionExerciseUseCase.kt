package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * Troca o exercício (ex.: aparelho ocupado) mantendo posição, alvo e quantidade de séries.
 * As séries são refeitas com os valores da última execução do novo exercício.
 */
class ReplaceSessionExerciseUseCase @Inject constructor(
    private val repository: SessionRepository,
    private val buildPrefilledSets: BuildPrefilledSetsUseCase,
) {
    suspend operator fun invoke(sessionId: Long, sessionExerciseId: Long, exerciseId: Long): SessionResult<Unit> {
        val session = repository.getEditable(sessionId) ?: return SessionResult.Failure(SessionError.NotFound)
        val current = session.findExercise(sessionExerciseId) ?: return SessionResult.Failure(SessionError.NotFound)
        if (current.exerciseId == exerciseId) return SessionResult.Success(Unit)
        if (session.exercises.any { it.exerciseId == exerciseId }) {
            return SessionResult.Failure(SessionError.DuplicateExercise)
        }

        val sets = buildPrefilledSets(exerciseId, current.sets.size.coerceAtLeast(1))
        repository.replaceExercise(sessionExerciseId, exerciseId, sets)
        return SessionResult.Success(Unit)
    }
}
