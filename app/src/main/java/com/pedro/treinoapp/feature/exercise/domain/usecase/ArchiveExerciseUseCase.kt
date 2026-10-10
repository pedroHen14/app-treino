package com.pedro.treinoapp.feature.exercise.domain.usecase

import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseError
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseResult
import com.pedro.treinoapp.feature.exercise.domain.repository.ExerciseRepository
import javax.inject.Inject

/** Vale para qualquer exercício, inclusive os do catálogo inicial. */
class ArchiveExerciseUseCase @Inject constructor(
    private val repository: ExerciseRepository,
) {
    suspend operator fun invoke(id: Long): ExerciseResult<Unit> {
        val exercise = repository.getById(id) ?: return ExerciseResult.Failure(ExerciseError.NotFound)
        if (!exercise.isArchived) repository.update(exercise.copy(isArchived = true))
        return ExerciseResult.Success(Unit)
    }
}
