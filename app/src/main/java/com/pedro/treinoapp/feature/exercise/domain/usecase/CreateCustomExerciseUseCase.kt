package com.pedro.treinoapp.feature.exercise.domain.usecase

import com.pedro.treinoapp.feature.exercise.domain.model.Exercise
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseResult
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import com.pedro.treinoapp.feature.exercise.domain.repository.ExerciseRepository
import javax.inject.Inject

class CreateCustomExerciseUseCase @Inject constructor(
    private val repository: ExerciseRepository,
    private val validateName: ValidateExerciseNameUseCase,
) {
    /** @return o id do exercício criado. */
    suspend operator fun invoke(name: String, muscleGroup: MuscleGroup): ExerciseResult<Long> =
        when (val validation = validateName(name)) {
            is ExerciseResult.Failure -> validation
            is ExerciseResult.Success -> {
                val exercise = Exercise(name = validation.value, muscleGroup = muscleGroup, isCustom = true)
                ExerciseResult.Success(repository.insert(exercise))
            }
        }
}
