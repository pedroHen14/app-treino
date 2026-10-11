package com.pedro.treinoapp.feature.session.data.local

import com.pedro.treinoapp.feature.session.domain.model.NewSessionExercise
import com.pedro.treinoapp.feature.session.domain.model.NewWorkoutSession
import com.pedro.treinoapp.feature.session.domain.model.SessionDay
import com.pedro.treinoapp.feature.session.domain.model.SessionExercise
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSession
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSet
import com.pedro.treinoapp.feature.workout.domain.model.RepRange

/** As linhas chegam ordenadas por exercício e série; a ordem é mantida no agrupamento. */
fun WorkoutSessionEntity.toDomain(rows: List<SessionSetRow>) = WorkoutSession(
    id = id,
    templateId = templateId,
    templateName = templateName,
    date = date,
    startedAt = startedAt,
    finishedAt = finishedAt,
    makeupForDate = makeupForDate,
    exercises = rows.groupBy { it.sessionExerciseId }.values.map { group ->
        val first = group.first()
        SessionExercise(
            id = first.sessionExerciseId,
            exerciseId = first.exerciseId,
            exerciseName = first.exerciseName,
            targetReps = if (first.targetRepsMin != null && first.targetRepsMax != null) {
                RepRange(first.targetRepsMin, first.targetRepsMax)
            } else {
                null
            },
            sets = group.mapNotNull { it.toSet() },
        )
    },
)

private fun SessionSetRow.toSet(): WorkoutSet? = setId?.let {
    WorkoutSet(
        id = it,
        weightGrams = weightGrams,
        reps = reps,
        isCompleted = isCompleted ?: false,
        completedAt = completedAt,
    )
}

fun SessionDayRow.toDomain() = SessionDay(date = date, templateId = templateId, makeupForDate = makeupForDate)

fun LastSetRow.toDomain() = WorkoutSet(weightGrams = weightGrams, reps = reps)

fun NewWorkoutSession.toEntity() = WorkoutSessionEntity(
    templateId = templateId,
    templateName = templateName,
    date = date,
    startedAt = startedAt,
    makeupForDate = makeupForDate,
)

/** `sessionId` e `position` são preenchidos pelo DAO ao gravar, quando não informados. */
fun NewSessionExercise.toEntity(sessionId: Long = 0) = SessionExerciseEntity(
    sessionId = sessionId,
    exerciseId = exerciseId,
    position = 0,
    targetRepsMin = targetReps?.min,
    targetRepsMax = targetReps?.max,
)

/** `sessionExerciseId` e `position` são preenchidos pelo DAO ao gravar, quando não informados. */
fun WorkoutSet.toEntity(sessionExerciseId: Long = 0) = WorkoutSetEntity(
    sessionExerciseId = sessionExerciseId,
    position = 0,
    weightGrams = weightGrams,
    reps = reps,
    isCompleted = isCompleted,
    completedAt = completedAt,
)
