package com.pedro.treinoapp.feature.weeklysummary.domain.usecase

import com.pedro.treinoapp.core.time.weekStart
import com.pedro.treinoapp.feature.history.domain.repository.HistoryRepository
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import com.pedro.treinoapp.feature.weeklysummary.domain.WeeklySummaryCalculator
import com.pedro.treinoapp.feature.weeklysummary.domain.model.WeeklySummary
import com.pedro.treinoapp.feature.workout.domain.usecase.ObserveWeeklyScheduleUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Resumo da semana corrente, recalculado a cada mudança na agenda, nas sessões ou no histórico.
 * Usa a agenda atual também para os dias que já passaram.
 */
class ObserveWeeklySummaryUseCase @Inject constructor(
    private val observeSchedule: ObserveWeeklyScheduleUseCase,
    private val sessions: SessionRepository,
    private val history: HistoryRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<WeeklySummary> {
        val today = LocalDate.now(clock)
        val weekStart = today.weekStart()
        return combine(
            observeSchedule(),
            sessions.observeFinishedSessionDays(weekStart, weekStart.plusDays(6)),
            history.observeCompletedSets(),
        ) { schedule, sessionDays, sets ->
            WeeklySummaryCalculator.calculate(today, schedule, sessionDays, sets)
        }
    }
}
