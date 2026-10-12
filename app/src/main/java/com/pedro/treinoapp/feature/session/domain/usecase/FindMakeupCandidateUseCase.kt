package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.core.time.weekStart
import com.pedro.treinoapp.feature.session.domain.model.SessionDay
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import com.pedro.treinoapp.feature.workout.domain.repository.WeeklyScheduleRepository
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Dia que o treino com [templateId] pode estar repondo, para o app perguntar ao iniciar.
 * Só vale quando o modelo não é o agendado para hoje e só olha dias anteriores da semana
 * corrente (segunda a domingo) em que esse modelo estava agendado e ficou pendente.
 * Com mais de um dia pendente, sugere o mais antigo. A agenda nunca é alterada.
 *
 * Um dia está cumprido se alguma sessão o repôs, ou se nele houve sessão do próprio modelo
 * que não era reposição de outro dia.
 */
class FindMakeupCandidateUseCase @Inject constructor(
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
            .filter { it < today }
            .sorted()
        if (candidates.isEmpty()) return null

        val days = sessions.getSessionDays(weekStart, today)
        return candidates.firstOrNull { !days.fulfill(it, templateId) }
    }

    private fun List<SessionDay>.fulfill(date: LocalDate, templateId: Long) = any {
        it.makeupForDate == date || (it.date == date && it.makeupForDate == null && it.templateId == templateId)
    }
}
