package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.repository.RestTimerSettingsRepository
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import com.pedro.treinoapp.feature.session.domain.timer.RestTimer
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject

/** Marca a série como concluída e inicia o descanso com o tempo padrão. Exige carga e repetições. */
class CompleteSetUseCase @Inject constructor(
    private val repository: SessionRepository,
    private val settings: RestTimerSettingsRepository,
    private val restTimer: RestTimer,
    private val clock: Clock,
) {
    suspend operator fun invoke(sessionId: Long, setId: Long): SessionResult<Unit> {
        val set = repository.getActive(sessionId)?.findSet(setId)
            ?: return SessionResult.Failure(SessionError.NotFound)
        if (set.isCompleted) return SessionResult.Success(Unit)
        if (!set.isFilled) return SessionResult.Failure(SessionError.MissingValues)
        validateSetValues(set.weightGrams, set.reps)?.let { return SessionResult.Failure(it) }

        repository.setCompleted(setId, clock.instant())
        restTimer.start(settings.observeDefaultDuration().first())
        return SessionResult.Success(Unit)
    }
}
