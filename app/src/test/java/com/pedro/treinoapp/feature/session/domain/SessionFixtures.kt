package com.pedro.treinoapp.feature.session.domain

import com.pedro.treinoapp.feature.session.domain.model.SessionExercise
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSession
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSet
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Quinta-feira, 15/10/2026, 18h UTC. */
val TODAY: LocalDate = LocalDate.of(2026, 10, 15)
val NOW: Instant = TODAY.atTime(18, 0).toInstant(ZoneOffset.UTC)
val fixedClock: Clock = Clock.fixed(NOW, ZoneOffset.UTC)

fun completedSet(id: Long, weightGrams: Int, reps: Int) =
    WorkoutSet(id = id, weightGrams = weightGrams, reps = reps, isCompleted = true, completedAt = NOW)

fun sessionExercise(id: Long, exerciseId: Long, vararg sets: WorkoutSet) =
    SessionExercise(id = id, exerciseId = exerciseId, exerciseName = "Exercício $exerciseId", sets = sets.toList())

fun session(
    id: Long,
    date: LocalDate = TODAY,
    templateId: Long? = null,
    finished: Boolean = true,
    makeupForDate: LocalDate? = null,
    vararg exercises: SessionExercise,
) = WorkoutSession(
    id = id,
    templateId = templateId,
    templateName = templateId?.let { "Modelo $it" },
    date = date,
    startedAt = date.atTime(8, 0).toInstant(ZoneOffset.UTC),
    finishedAt = if (finished) date.atTime(9, 0).toInstant(ZoneOffset.UTC) else null,
    makeupForDate = makeupForDate,
    exercises = exercises.toList(),
)
