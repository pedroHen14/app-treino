package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.core.time.weekEnd
import com.pedro.treinoapp.core.time.weekStart
import com.pedro.treinoapp.feature.session.domain.model.SessionDay
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import com.pedro.treinoapp.feature.workout.domain.repository.WeeklyScheduleRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Dia que o treino com [templateId] pode estar adiantando, para o app perguntar ao iniciar.
 * Só vale quando o modelo não é o agendado para hoje e só olha dias posteriores da semana
 * corrente (até domingo) em que esse modelo está agendado e que ainda não foram cumpridos.
 * Com mais de um dia, sugere o mais próximo. A agenda nunca é alterada.
 */
class FindAdvanceCandidateUseCase @Inject constructor(
    private val schedule: WeeklyScheduleRepository,
    private val sessions: SessionRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(templateId: Long): LocalDate? {
        val today = LocalDate.now(clock)
        val scheduledDays = schedule.getDaysFor(templateId)
        if (today.dayOfWeek in scheduledDays) return null

        val weekStart = today.weekStart()
        val candidates = scheduledDays
            .map { weekStart.plusDays(it.value - 1L) }
            .filter { it > today }
            .sorted()
        if (candidates.isEmpty()) return null

        val days = sessions.getSessionDays(weekStart, today.weekEnd())
        return candidates.firstOrNull { !days.fulfill(it, templateId) }
    }

    private fun List<SessionDay>.fulfill(date: LocalDate, templateId: Long) = any {
        it.makeupForDate == date || (it.date == date && it.makeupForDate == null && it.templateId == templateId)
    }
}
