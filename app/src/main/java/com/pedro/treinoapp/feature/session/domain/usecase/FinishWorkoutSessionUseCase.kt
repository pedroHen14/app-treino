package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import com.pedro.treinoapp.feature.session.domain.timer.RestTimer
import java.time.Clock
import javax.inject.Inject

/**
 * Salva a sessão no histórico. Séries não concluídas são descartadas, assim como exercícios que
 * ficarem sem séries; a tela avisa antes, usando `WorkoutSession.pendingSetCount`.
 * Sem nenhuma série concluída não há o que salvar: a saída é descartar a sessão.
 */
class FinishWorkoutSessionUseCase @Inject constructor(
    private val repository: SessionRepository,
    private val restTimer: RestTimer,
    private val clock: Clock,
) {
    suspend operator fun invoke(sessionId: Long): SessionResult<Unit> {
        val session = repository.getActive(sessionId) ?: return SessionResult.Failure(SessionError.NotFound)
        if (session.completedSetCount == 0) return SessionResult.Failure(SessionError.NoCompletedSets)
        repository.finish(sessionId, clock.instant())
        restTimer.skip()
        return SessionResult.Success(Unit)
    }
}
