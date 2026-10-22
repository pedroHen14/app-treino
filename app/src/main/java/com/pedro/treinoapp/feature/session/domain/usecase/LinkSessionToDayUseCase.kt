package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.model.isLinkableDay
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Corrige no histórico o dia da agenda que uma sessão finalizada cumpre: reposição de um dia anterior
 * ou adiantamento de um posterior, sempre na semana da sessão. Nulo deixa a sessão sem vínculo.
 * Só sessões de um modelo podem ser vinculadas, como ao iniciar.
 */
class LinkSessionToDayUseCase @Inject constructor(
    private val repository: SessionRepository,
) {
    suspend operator fun invoke(sessionId: Long, date: LocalDate?): SessionResult<Unit> {
        val session = repository.get(sessionId)
        if (session == null || session.isActive) return SessionResult.Failure(SessionError.NotFound)
        if (date != null && (session.templateId == null || !isLinkableDay(date, session.date))) {
            return SessionResult.Failure(SessionError.InvalidMakeupDate)
        }
        repository.setMakeupForDate(sessionId, date)
        return SessionResult.Success(Unit)
    }
}
