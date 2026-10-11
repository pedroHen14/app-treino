package com.pedro.treinoapp.feature.workout.domain

import com.pedro.treinoapp.feature.workout.domain.repository.WeeklyScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek

class FakeWeeklyScheduleRepository(initial: Map<DayOfWeek, Long?> = emptyMap()) : WeeklyScheduleRepository {

    private val days = MutableStateFlow(initial)

    val current: Map<DayOfWeek, Long?> get() = days.value

    override fun observe(): Flow<Map<DayOfWeek, Long?>> = days

    override suspend fun getDaysFor(templateId: Long): List<DayOfWeek> =
        days.value.filterValues { it == templateId }.keys.toList()

    override suspend fun setDay(dayOfWeek: DayOfWeek, templateId: Long?) {
        days.update { it + (dayOfWeek to templateId) }
    }

    override suspend fun clearTemplate(templateId: Long) {
        days.update { map -> map.mapValues { (_, id) -> id.takeIf { it != templateId } } }
    }
}
