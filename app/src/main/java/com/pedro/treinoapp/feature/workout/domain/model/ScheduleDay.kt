package com.pedro.treinoapp.feature.workout.domain.model

import java.time.DayOfWeek

/** Um dia da agenda semanal. Sem modelo, o dia é de descanso. */
data class ScheduleDay(
    val dayOfWeek: DayOfWeek,
    val templateId: Long? = null,
    val templateName: String? = null,
) {
    val isRest: Boolean get() = templateId == null
}
