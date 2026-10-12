package com.pedro.treinoapp.feature.history.domain

import com.pedro.treinoapp.feature.history.domain.model.FinishedSession
import com.pedro.treinoapp.feature.history.domain.model.RecordedSet
import java.time.LocalDate
import java.time.ZoneOffset

val DAY_1: LocalDate = LocalDate.of(2026, 10, 5)
val DAY_2: LocalDate = LocalDate.of(2026, 10, 8)
val DAY_3: LocalDate = LocalDate.of(2026, 10, 12)

/** Sessão finalizada das 8h às 9h (UTC) do dia. */
fun finishedSession(id: Long, date: LocalDate, exerciseCount: Int = 1, templateName: String? = "Upper") =
    FinishedSession(
        id = id,
        templateName = templateName,
        date = date,
        startedAt = date.atTime(8, 0).toInstant(ZoneOffset.UTC),
        finishedAt = date.atTime(9, 0).toInstant(ZoneOffset.UTC),
        makeupForDate = null,
        exerciseCount = exerciseCount,
    )

fun recordedSet(
    id: Long,
    weightGrams: Int,
    reps: Int,
    date: LocalDate = DAY_1,
    sessionId: Long = 1,
    exerciseId: Long = 1,
) = RecordedSet(
    setId = id,
    sessionId = sessionId,
    exerciseId = exerciseId,
    date = date,
    weightGrams = weightGrams,
    reps = reps,
)
