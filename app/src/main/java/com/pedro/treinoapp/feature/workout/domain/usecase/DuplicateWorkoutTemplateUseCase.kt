package com.pedro.treinoapp.feature.workout.domain.usecase

import com.pedro.treinoapp.core.text.toNameKey
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutError
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutResult
import com.pedro.treinoapp.feature.workout.domain.repository.WorkoutTemplateRepository
import javax.inject.Inject

/**
 * Cria uma cópia do modelo com os mesmos exercícios. Os dias da agenda não são copiados.
 * O nome vem de [copyName] (o texto fica nos resources), tentando a cópia 1, 2, 3…
 * até achar um nome livre.
 */
class DuplicateWorkoutTemplateUseCase @Inject constructor(
    private val repository: WorkoutTemplateRepository,
) {
    /** @return o id do novo modelo. */
    suspend operator fun invoke(
        id: Long,
        copyName: (original: String, copyNumber: Int) -> String,
    ): WorkoutResult<Long> {
        val source = repository.getById(id) ?: return WorkoutResult.Failure(WorkoutError.NotFound)
        val takenKeys = repository.getSummaries().mapTo(mutableSetOf()) { it.name.toNameKey() }
        val name = generateSequence(1) { it + 1 }
            .map { copyName(source.name, it) }
            .first { it.toNameKey() !in takenKeys }
        return WorkoutResult.Success(repository.insert(source.copy(id = 0, name = name)))
    }
}
