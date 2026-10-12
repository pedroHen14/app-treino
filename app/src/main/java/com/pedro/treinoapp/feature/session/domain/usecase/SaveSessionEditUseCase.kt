package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * Encerra a edição de uma sessão finalizada. No histórico toda série é concluída: as preenchidas
 * durante a edição passam a contar, com o horário de término da sessão; as vazias são descartadas,
 * assim como exercícios que ficarem sem séries. Sem nenhuma série a sessão não fica salva: a saída é excluí-la.
 */
class SaveSessionEditUseCase @Inject constructor(
    private val repository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: Long): SessionResult<Unit> {
        val session = repository.getEditable(sessionId) ?: return SessionResult.Failure(SessionError.NotFound)
        val finishedAt = session.finishedAt ?: return SessionResult.Failure(SessionError.NotFound)
        val keptSets = session.exercises.sumOf { exercise -> exercise.sets.count { it.isCompleted || it.isFilled } }
        if (keptSets == 0) return SessionResult.Failure(SessionError.NoCompletedSets)

        repository.saveEdit(sessionId, completedAt = finishedAt)
        return SessionResult.Success(Unit)
    }
}
