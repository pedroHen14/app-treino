package com.pedro.treinoapp.feature.session.domain.model

import java.time.LocalDate

/**
 * O que a tela Hoje mostra sobre treino: o modelo agendado (nulo = descanso), se ele já foi
 * feito hoje e a sessão em andamento, se houver.
 */
data class TodayPlan(
    val date: LocalDate,
    val scheduledTemplateId: Long?,
    val scheduledTemplateName: String?,
    val isScheduledDone: Boolean,
    val activeSessionId: Long?,
    val activeSessionName: String?,
) {
    val isRestDay: Boolean get() = scheduledTemplateId == null
}
