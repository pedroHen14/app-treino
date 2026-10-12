package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import com.pedro.treinoapp.feature.session.domain.timer.RestTimer
import javax.inject.Inject

/** Cancela a sessão em andamento sem registrar nada no histórico. */
class DiscardWorkoutSessionUseCase @Inject constructor(
    private val repository: SessionRepository,
    private val restTimer: RestTimer,
) {
    suspend operator fun invoke(sessionId: Long): SessionResult<Unit> {
        repository.getActive(sessionId) ?: return SessionResult.Failure(SessionError.NotFound)
        repository.delete(sessionId)
        restTimer.skip()
        return SessionResult.Success(Unit)
    }
}
