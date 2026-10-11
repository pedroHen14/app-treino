package com.pedro.treinoapp.feature.workout.domain

import com.pedro.treinoapp.feature.workout.domain.model.RepRange
import com.pedro.treinoapp.feature.workout.domain.model.TemplateExercise
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutTemplate

fun templateExercise(
    exerciseId: Long,
    sets: Int = 3,
    reps: RepRange = RepRange(8, 12),
) = TemplateExercise(
    exerciseId = exerciseId,
    exerciseName = "Exercício $exerciseId",
    targetSets = sets,
    targetReps = reps,
)

fun template(id: Long, name: String, vararg exerciseIds: Long) = WorkoutTemplate(
    id = id,
    name = name,
    exercises = exerciseIds.map { templateExercise(it) },
)
