package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.model.TodayPlan
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import com.pedro.treinoapp.feature.workout.domain.usecase.ObserveWeeklyScheduleUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Treino previsto para hoje na agenda, ou descanso, e a sessão em andamento. */
class ObserveTodayPlanUseCase @Inject constructor(
    private val observeSchedule: ObserveWeeklyScheduleUseCase,
    private val sessions: SessionRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<TodayPlan> {
        val today = LocalDate.now(clock)
        return combine(
            observeSchedule(),
            sessions.observeActive(),
            sessions.observeFinishedTemplateIdsOn(today),
        ) { schedule, active, finishedTemplateIds ->
            val day = schedule.first { it.dayOfWeek == today.dayOfWeek }
            TodayPlan(
                date = today,
                scheduledTemplateId = day.templateId,
                scheduledTemplateName = day.templateName,
                isScheduledDone = day.templateId != null && day.templateId in finishedTemplateIds,
                activeSessionId = active?.id,
                activeSessionName = active?.templateName,
            )
        }
    }
}
