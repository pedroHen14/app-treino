package com.pedro.treinoapp.feature.workout.domain.usecase

import com.pedro.treinoapp.feature.workout.domain.model.TemplateExercise
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutResult
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutTemplate
import com.pedro.treinoapp.feature.workout.domain.repository.WorkoutTemplateRepository
import javax.inject.Inject

class CreateWorkoutTemplateUseCase @Inject constructor(
    private val repository: WorkoutTemplateRepository,
    private val validate: ValidateWorkoutTemplateUseCase,
) {
    /** @return o id do modelo criado. A ordem de [exercises] é a ordem do modelo. */
    suspend operator fun invoke(name: String, exercises: List<TemplateExercise>): WorkoutResult<Long> =
        when (val validation = validate(name, exercises)) {
            is WorkoutResult.Failure -> validation
            is WorkoutResult.Success -> {
                val template = WorkoutTemplate(name = validation.value, exercises = exercises)
                WorkoutResult.Success(repository.insert(template))
            }
        }
}
