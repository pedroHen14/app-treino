package com.pedro.treinoapp.feature.workout.domain.model

/** Exercício dentro de um modelo. A posição é a ordem na lista do modelo. */
data class TemplateExercise(
    val exerciseId: Long,
    val exerciseName: String,
    val targetSets: Int,
    val targetReps: RepRange,
) {
    companion object {
        const val MIN_SETS = 1
        const val MAX_SETS = 20
        const val DEFAULT_SETS = 3
        val DEFAULT_REPS = RepRange(8, 12)
    }
}
