package com.pedro.treinoapp.feature.exercise.domain.usecase

import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseError
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseResult
import com.pedro.treinoapp.feature.exercise.domain.repository.ExerciseRepository
import javax.inject.Inject

/**
 * Só exercícios personalizados que nunca foram usados podem ser excluídos.
 * Se já foi usado em sessão ou em modelo de treino, a saída é arquivar.
 */
class DeleteExerciseUseCase @Inject constructor(
    private val repository: ExerciseRepository,
) {
    suspend operator fun invoke(id: Long): ExerciseResult<Unit> {
        val exercise = repository.getById(id) ?: return ExerciseResult.Failure(ExerciseError.NotFound)
        if (!exercise.isCustom) return ExerciseResult.Failure(ExerciseError.NotCustom)
        if (repository.isUsedInSession(id)) return ExerciseResult.Failure(ExerciseError.UsedInSession)
        if (repository.isUsedInTemplate(id)) return ExerciseResult.Failure(ExerciseError.UsedInTemplate)

        repository.delete(id)
        return ExerciseResult.Success(Unit)
    }
}
