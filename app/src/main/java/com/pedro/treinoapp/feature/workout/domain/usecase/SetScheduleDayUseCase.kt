package com.pedro.treinoapp.feature.workout.domain.usecase

import com.pedro.treinoapp.feature.workout.domain.model.WorkoutError
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutResult
import com.pedro.treinoapp.feature.workout.domain.repository.WeeklyScheduleRepository
import com.pedro.treinoapp.feature.workout.domain.repository.WorkoutTemplateRepository
import java.time.DayOfWeek
import javax.inject.Inject

/** Define o modelo do dia, ou descanso com `templateId` nulo. Um modelo pode estar em vários dias. */
class SetScheduleDayUseCase @Inject constructor(
    private val schedule: WeeklyScheduleRepository,
    private val templates: WorkoutTemplateRepository,
) {
    suspend operator fun invoke(dayOfWeek: DayOfWeek, templateId: Long?): WorkoutResult<Unit> {
        if (templateId != null && templates.getById(templateId) == null) {
            return WorkoutResult.Failure(WorkoutError.NotFound)
        }
        schedule.setDay(dayOfWeek, templateId)
        return WorkoutResult.Success(Unit)
    }
}
