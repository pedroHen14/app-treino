package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import javax.inject.Inject

/** Desfaz a conclusão da série. O descanso em andamento, se houver, continua. */
class UncompleteSetUseCase @Inject constructor(
    private val repository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: Long, setId: Long): SessionResult<Unit> {
        repository.getActive(sessionId)?.findSet(setId) ?: return SessionResult.Failure(SessionError.NotFound)
        repository.setCompleted(setId, completedAt = null)
        return SessionResult.Success(Unit)
    }
}
