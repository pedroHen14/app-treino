package com.pedro.treinoapp.feature.workout.domain.usecase

import com.pedro.treinoapp.feature.workout.domain.model.WorkoutError
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutResult
import com.pedro.treinoapp.feature.workout.domain.repository.WeeklyScheduleRepository
import com.pedro.treinoapp.feature.workout.domain.repository.WorkoutTemplateRepository
import javax.inject.Inject

/**
 * Exclui o modelo; os dias da agenda que o usavam viram descanso. A confirmação do usuário
 * acontece antes, na tela. Sessões já registradas continuam no histórico com o nome copiado.
 */
class DeleteWorkoutTemplateUseCase @Inject constructor(
    private val templates: WorkoutTemplateRepository,
    private val schedule: WeeklyScheduleRepository,
) {
    suspend operator fun invoke(id: Long): WorkoutResult<Unit> {
        templates.getById(id) ?: return WorkoutResult.Failure(WorkoutError.NotFound)
        schedule.clearTemplate(id)
        templates.delete(id)
        return WorkoutResult.Success(Unit)
    }
}
