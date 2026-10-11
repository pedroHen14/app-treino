package com.pedro.treinoapp.feature.workout.domain.usecase

import com.pedro.treinoapp.feature.workout.domain.model.ScheduleDay
import com.pedro.treinoapp.feature.workout.domain.repository.WeeklyScheduleRepository
import com.pedro.treinoapp.feature.workout.domain.repository.WorkoutTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import javax.inject.Inject

/** Sempre os sete dias, de segunda a domingo. Dia sem modelo é descanso. */
class ObserveWeeklyScheduleUseCase @Inject constructor(
    private val schedule: WeeklyScheduleRepository,
    private val templates: WorkoutTemplateRepository,
) {
    operator fun invoke(): Flow<List<ScheduleDay>> =
        combine(schedule.observe(), templates.observeSummaries()) { days, summaries ->
            val names = summaries.associate { it.id to it.name }
            DayOfWeek.entries.map { day ->
                val templateId = days[day]?.takeIf { it in names }
                ScheduleDay(day, templateId, templateId?.let(names::get))
            }
        }
}
