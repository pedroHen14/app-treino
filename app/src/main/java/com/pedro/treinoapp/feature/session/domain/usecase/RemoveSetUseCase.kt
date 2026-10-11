package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import javax.inject.Inject

class RemoveSetUseCase @Inject constructor(
    private val repository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: Long, setId: Long): SessionResult<Unit> {
        repository.getEditable(sessionId)?.findSet(setId) ?: return SessionResult.Failure(SessionError.NotFound)
        repository.removeSet(setId)
        return SessionResult.Success(Unit)
    }
}
