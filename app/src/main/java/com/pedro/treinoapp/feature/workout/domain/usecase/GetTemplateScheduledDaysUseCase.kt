package com.pedro.treinoapp.feature.workout.domain.usecase

import com.pedro.treinoapp.feature.workout.domain.repository.WeeklyScheduleRepository
import java.time.DayOfWeek
import javax.inject.Inject

/** Dias que viram descanso se o modelo for excluído; usado na confirmação da exclusão. */
class GetTemplateScheduledDaysUseCase @Inject constructor(
    private val schedule: WeeklyScheduleRepository,
) {
    suspend operator fun invoke(templateId: Long): List<DayOfWeek> = schedule.getDaysFor(templateId).sorted()
}
