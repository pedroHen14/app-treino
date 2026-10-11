package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import javax.inject.Inject

/** Remove o exercício da sessão com todas as séries. O modelo de origem não muda. */
class RemoveSessionExerciseUseCase @Inject constructor(
    private val repository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: Long, sessionExerciseId: Long): SessionResult<Unit> {
        repository.getEditable(sessionId)?.findExercise(sessionExerciseId)
            ?: return SessionResult.Failure(SessionError.NotFound)
        repository.removeExercise(sessionExerciseId)
        return SessionResult.Success(Unit)
    }
}
