package com.pedro.treinoapp.feature.workout.domain.usecase

import com.pedro.treinoapp.feature.workout.domain.model.TemplateExercise
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutError
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutResult
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutTemplate
import com.pedro.treinoapp.feature.workout.domain.repository.WorkoutTemplateRepository
import javax.inject.Inject

/**
 * Troca nome e exercícios do modelo. Sessões já registradas guardam a própria cópia
 * do que foi feito, então não mudam.
 */
class UpdateWorkoutTemplateUseCase @Inject constructor(
    private val repository: WorkoutTemplateRepository,
    private val validate: ValidateWorkoutTemplateUseCase,
) {
    suspend operator fun invoke(
        id: Long,
        name: String,
        exercises: List<TemplateExercise>,
    ): WorkoutResult<Unit> {
        repository.getById(id) ?: return WorkoutResult.Failure(WorkoutError.NotFound)
        return when (val validation = validate(name, exercises, excludingId = id)) {
            is WorkoutResult.Failure -> validation
            is WorkoutResult.Success -> {
                repository.update(WorkoutTemplate(id = id, name = validation.value, exercises = exercises))
                WorkoutResult.Success(Unit)
            }
        }
    }
}
