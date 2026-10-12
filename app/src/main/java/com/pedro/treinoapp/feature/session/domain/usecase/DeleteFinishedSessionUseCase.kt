package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import javax.inject.Inject

/** Remove uma sessão do histórico. A sessão em andamento sai por [DiscardWorkoutSessionUseCase]. */
class DeleteFinishedSessionUseCase @Inject constructor(
    private val repository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: Long): SessionResult<Unit> {
        val session = repository.get(sessionId)
        if (session == null || session.isActive) return SessionResult.Failure(SessionError.NotFound)
        repository.delete(sessionId)
        return SessionResult.Success(Unit)
    }
}
