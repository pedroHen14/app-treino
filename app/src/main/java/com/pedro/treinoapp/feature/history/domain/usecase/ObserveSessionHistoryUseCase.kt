package com.pedro.treinoapp.feature.history.domain.usecase

import com.pedro.treinoapp.feature.history.domain.PersonalRecordCalculator
import com.pedro.treinoapp.feature.history.domain.model.RecordedSet
import com.pedro.treinoapp.feature.history.domain.model.SessionSummary
import com.pedro.treinoapp.feature.history.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Duration
import javax.inject.Inject

/**
 * Sessões finalizadas, da mais recente para a mais antiga, com duração, quantidade de exercícios e
 * quantas séries bateram recorde. Os recordes são recalculados a cada mudança no histórico.
 */
class ObserveSessionHistoryUseCase @Inject constructor(
    private val repository: HistoryRepository,
) {
    operator fun invoke(): Flow<List<SessionSummary>> =
        combine(repository.observeFinishedSessions(), repository.observeCompletedSets()) { sessions, sets ->
            val recordsBySession = countRecordsBySession(sets)
            sessions.map { session ->
                SessionSummary(
                    id = session.id,
                    templateName = session.templateName,
                    date = session.date,
                    duration = Duration.between(session.startedAt, session.finishedAt),
                    makeupForDate = session.makeupForDate,
                    exerciseCount = session.exerciseCount,
                    recordCount = recordsBySession[session.id] ?: 0,
                )
            }
        }

    private fun countRecordsBySession(sets: List<RecordedSet>): Map<Long, Int> {
        val sessionBySet = sets.associate { it.setId to it.sessionId }
        return sets.groupBy { it.exerciseId }.values
            .flatMap { PersonalRecordCalculator.analyze(it).recordsBySet.keys }
            .groupingBy { sessionBySet.getValue(it) }
            .eachCount()
    }
}
