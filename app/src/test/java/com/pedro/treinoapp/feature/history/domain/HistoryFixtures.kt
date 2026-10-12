package com.pedro.treinoapp.feature.history.domain

import com.pedro.treinoapp.feature.history.domain.model.RecordedSet
import java.time.LocalDate

val DAY_1: LocalDate = LocalDate.of(2026, 10, 5)
val DAY_2: LocalDate = LocalDate.of(2026, 10, 8)
val DAY_3: LocalDate = LocalDate.of(2026, 10, 12)

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
