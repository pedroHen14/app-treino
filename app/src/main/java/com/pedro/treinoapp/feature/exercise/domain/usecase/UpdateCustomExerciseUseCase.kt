package com.pedro.treinoapp.feature.exercise.domain.usecase

import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseError
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseResult
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import com.pedro.treinoapp.feature.exercise.domain.repository.ExerciseRepository
import javax.inject.Inject

class UpdateCustomExerciseUseCase @Inject constructor(
    private val repository: ExerciseRepository,
    private val validateName: ValidateExerciseNameUseCase,
) {
    suspend operator fun invoke(id: Long, name: String, muscleGroup: MuscleGroup): ExerciseResult<Unit> {
        val exercise = repository.getById(id) ?: return ExerciseResult.Failure(ExerciseError.NotFound)
        if (!exercise.isCustom) return ExerciseResult.Failure(ExerciseError.NotCustom)

        return when (val validation = validateName(name, excludingId = id)) {
            is ExerciseResult.Failure -> validation
            is ExerciseResult.Success -> {
                repository.update(exercise.copy(name = validation.value, muscleGroup = muscleGroup))
                ExerciseResult.Success(Unit)
            }
        }
    }
}
