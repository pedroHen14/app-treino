package com.pedro.treinoapp.feature.workout.data.local

import com.pedro.treinoapp.feature.workout.domain.model.RepRange
import com.pedro.treinoapp.feature.workout.domain.model.TemplateExercise
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutTemplate
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutTemplateSummary

fun TemplateSummaryRow.toDomain() = WorkoutTemplateSummary(
    id = id,
    name = name,
    exerciseCount = exerciseCount,
)

fun TemplateExerciseRow.toDomain() = TemplateExercise(
    exerciseId = entity.exerciseId,
    exerciseName = exerciseName,
    targetSets = entity.targetSets,
    targetReps = RepRange(entity.targetRepsMin, entity.targetRepsMax),
)

fun WorkoutTemplateEntity.toDomain(exercises: List<TemplateExerciseRow>) = WorkoutTemplate(
    id = id,
    name = name,
    exercises = exercises.map { it.toDomain() },
)

/** `templateId` e `position` são preenchidos pelo DAO ao gravar. */
fun TemplateExercise.toEntity() = TemplateExerciseEntity(
    templateId = 0,
    exerciseId = exerciseId,
    position = 0,
    targetSets = targetSets,
    targetRepsMin = targetReps.min,
    targetRepsMax = targetReps.max,
)
