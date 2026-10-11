package com.pedro.treinoapp.feature.session.domain.model

import com.pedro.treinoapp.feature.workout.domain.model.RepRange
import java.time.Instant
import java.time.LocalDate

/**
 * Sessão de treino. Guarda o que foi executado, independente do modelo de origem:
 * `templateName` é uma cópia e `templateId` vira nulo se o modelo for excluído.
 * `finishedAt` nulo indica sessão em andamento; `makeupForDate` é o dia reposto, se for reposição.
 */
data class WorkoutSession(
    val id: Long,
    val templateId: Long?,
    val templateName: String?,
    val date: LocalDate,
    val startedAt: Instant,
    val finishedAt: Instant? = null,
    val makeupForDate: LocalDate? = null,
    val exercises: List<SessionExercise> = emptyList(),
) {
    val isActive: Boolean get() = finishedAt == null

    val completedSetCount: Int get() = exercises.sumOf { exercise -> exercise.sets.count { it.isCompleted } }

    val pendingSetCount: Int get() = exercises.sumOf { exercise -> exercise.sets.count { !it.isCompleted } }

    fun findExercise(sessionExerciseId: Long): SessionExercise? = exercises.find { it.id == sessionExerciseId }

    fun findSet(setId: Long): WorkoutSet? = exercises.firstNotNullOfOrNull { exercise -> exercise.sets.find { it.id == setId } }
}

/** `targetReps` vem do modelo e serve de dica quando a série ainda está vazia. */
data class SessionExercise(
    val id: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val targetReps: RepRange? = null,
    val sets: List<WorkoutSet> = emptyList(),
)

/** Carga em gramas. Valores nulos indicam série ainda não preenchida. */
data class WorkoutSet(
    val id: Long = 0,
    val weightGrams: Int? = null,
    val reps: Int? = null,
    val isCompleted: Boolean = false,
    val completedAt: Instant? = null,
) {
    val isFilled: Boolean get() = weightGrams != null && reps != null

    companion object {
        /** Incremento mínimo de carga: 0,5 kg. */
        const val WEIGHT_STEP_GRAMS = 500
        const val MAX_WEIGHT_GRAMS = 1_000_000
        const val MIN_REPS = 1
        const val MAX_REPS = 999
        const val MAX_SETS = 20
    }
}

/** Dados para criar uma sessão; ids e nomes são definidos ao gravar. */
data class NewWorkoutSession(
    val templateId: Long?,
    val templateName: String?,
    val date: LocalDate,
    val startedAt: Instant,
    val makeupForDate: LocalDate? = null,
    val exercises: List<NewSessionExercise> = emptyList(),
)

data class NewSessionExercise(
    val exerciseId: Long,
    val targetReps: RepRange? = null,
    val sets: List<WorkoutSet>,
)

/** Resumo usado para saber quais dias da semana já tiveram treino. */
data class SessionDay(
    val date: LocalDate,
    val templateId: Long?,
    val makeupForDate: LocalDate?,
)
