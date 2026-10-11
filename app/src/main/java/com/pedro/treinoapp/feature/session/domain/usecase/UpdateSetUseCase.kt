package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import javax.inject.Inject

/** Ajusta carga (gramas) e repetições. Série concluída não pode ficar sem valores. */
class UpdateSetUseCase @Inject constructor(
    private val repository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: Long, setId: Long, weightGrams: Int?, reps: Int?): SessionResult<Unit> {
        val set = repository.getEditable(sessionId)?.findSet(setId)
            ?: return SessionResult.Failure(SessionError.NotFound)
        validateSetValues(weightGrams, reps)?.let { return SessionResult.Failure(it) }
        if (set.isCompleted && (weightGrams == null || reps == null)) {
            return SessionResult.Failure(SessionError.MissingValues)
        }
        repository.updateSet(setId, weightGrams, reps)
        return SessionResult.Success(Unit)
    }
}
