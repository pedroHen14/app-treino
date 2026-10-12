package com.pedro.treinoapp.feature.history.data.local

import com.pedro.treinoapp.feature.history.domain.model.ExerciseWithHistory
import com.pedro.treinoapp.feature.history.domain.model.FinishedSession
import com.pedro.treinoapp.feature.history.domain.model.RecordedSet

fun FinishedSessionRow.toDomain() = FinishedSession(
    id = id,
    templateName = templateName,
    date = date,
    startedAt = startedAt,
    finishedAt = finishedAt,
    makeupForDate = makeupForDate,
    exerciseCount = exerciseCount,
)

fun CompletedSetRow.toDomain() = RecordedSet(
    setId = setId,
    sessionId = sessionId,
    exerciseId = exerciseId,
    date = date,
    weightGrams = weightGrams,
    reps = reps,
)

fun ExerciseHistoryRow.toDomain() = ExerciseWithHistory(
    exerciseId = id,
    name = name,
    lastDate = lastDate,
    sessionCount = sessionCount,
)
