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

        /**
         * Converte o texto do editor. Séries e reps mín. vazias assumem a sugestão
         * ([DEFAULT_SETS] e o mínimo de [DEFAULT_REPS]); reps máx. vazia significa valor fixo.
         * Texto inválido vira 0, que a validação rejeita com a mensagem certa.
         */
        fun fromInput(
            exerciseId: Long,
            exerciseName: String,
            sets: String,
            repsMin: String,
            repsMax: String,
        ): TemplateExercise {
            val min = repsMin.toIntOr(DEFAULT_REPS.min)
            return TemplateExercise(
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                targetSets = sets.toIntOr(DEFAULT_SETS),
                targetReps = RepRange(min, repsMax.toIntOr(min)),
            )
        }

        private fun String.toIntOr(whenEmpty: Int): Int {
            val text = trim()
            return if (text.isEmpty()) whenEmpty else text.toIntOrNull() ?: 0
        }
    }
}
