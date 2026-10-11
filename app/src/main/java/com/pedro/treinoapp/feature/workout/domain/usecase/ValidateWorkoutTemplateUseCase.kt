package com.pedro.treinoapp.feature.workout.domain.usecase

import com.pedro.treinoapp.core.text.normalizeName
import com.pedro.treinoapp.core.text.toNameKey
import com.pedro.treinoapp.feature.workout.domain.model.RepRange
import com.pedro.treinoapp.feature.workout.domain.model.TemplateExercise
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutError
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutResult
import com.pedro.treinoapp.feature.workout.domain.repository.WorkoutTemplateRepository
import javax.inject.Inject

/**
 * Modelo precisa de nome (único, sem diferenciar maiúsculas) e de pelo menos um exercício.
 * Cada exercício aparece uma vez só, com séries e repetições dentro dos limites; repetições
 * aceitam valor fixo (`min == max`) ou faixa (`min < max`).
 *
 * @return o nome normalizado, pronto para salvar.
 */
class ValidateWorkoutTemplateUseCase @Inject constructor(
    private val repository: WorkoutTemplateRepository,
) {
    suspend operator fun invoke(
        rawName: String,
        exercises: List<TemplateExercise>,
        excludingId: Long? = null,
    ): WorkoutResult<String> {
        val name = rawName.normalizeName()
        if (name.isEmpty()) return WorkoutResult.Failure(WorkoutError.NameBlank)

        val key = name.toNameKey()
        val taken = repository.getSummaries().any { it.id != excludingId && it.name.toNameKey() == key }
        if (taken) return WorkoutResult.Failure(WorkoutError.NameAlreadyExists)

        if (exercises.isEmpty()) return WorkoutResult.Failure(WorkoutError.NoExercises)

        val seen = mutableSetOf<Long>()
        exercises.forEachIndexed { index, exercise ->
            val error = when {
                !seen.add(exercise.exerciseId) -> WorkoutError.DuplicateExercise(index)
                exercise.targetSets !in TemplateExercise.MIN_SETS..TemplateExercise.MAX_SETS ->
                    WorkoutError.InvalidSets(index)
                !exercise.targetReps.isValid() -> WorkoutError.InvalidReps(index)
                else -> null
            }
            if (error != null) return WorkoutResult.Failure(error)
        }

        return WorkoutResult.Success(name)
    }

    private fun RepRange.isValid(): Boolean {
        val limits = RepRange.MIN_REPS..RepRange.MAX_REPS
        return min in limits && max in limits && min <= max
    }
}
