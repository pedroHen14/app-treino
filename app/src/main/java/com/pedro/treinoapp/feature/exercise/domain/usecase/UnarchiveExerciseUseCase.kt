package com.pedro.treinoapp.feature.exercise.domain.usecase

import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseError
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseResult
import com.pedro.treinoapp.feature.exercise.domain.repository.ExerciseRepository
import javax.inject.Inject

class UnarchiveExerciseUseCase @Inject constructor(
    private val repository: ExerciseRepository,
) {
    suspend operator fun invoke(id: Long): ExerciseResult<Unit> {
        val exercise = repository.getById(id) ?: return ExerciseResult.Failure(ExerciseError.NotFound)
        if (exercise.isArchived) repository.update(exercise.copy(isArchived = false))
        return ExerciseResult.Success(Unit)
    }
}
