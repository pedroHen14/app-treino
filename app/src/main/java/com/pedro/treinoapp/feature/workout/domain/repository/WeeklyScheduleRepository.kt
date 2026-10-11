package com.pedro.treinoapp.feature.workout.domain.repository

import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek

/** Dia ausente do mapa ou com valor nulo é descanso. */
interface WeeklyScheduleRepository {
    fun observe(): Flow<Map<DayOfWeek, Long?>>
    suspend fun getDaysFor(templateId: Long): List<DayOfWeek>
    suspend fun setDay(dayOfWeek: DayOfWeek, templateId: Long?)

    /** Transforma em descanso todos os dias que usam o modelo. */
    suspend fun clearTemplate(templateId: Long)
}
