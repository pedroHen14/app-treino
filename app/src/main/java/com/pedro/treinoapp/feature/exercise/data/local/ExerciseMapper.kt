package com.pedro.treinoapp.feature.exercise.data.local

import com.pedro.treinoapp.feature.exercise.domain.model.Exercise
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup

fun ExerciseEntity.toDomain() = Exercise(
    id = id,
    name = name,
    muscleGroup = MuscleGroup.valueOf(muscleGroup),
    isCustom = isCustom,
    isArchived = isArchived,
    description = description,
    wgerId = wgerId,
)

fun Exercise.toEntity() = ExerciseEntity(
    id = id,
    name = name,
    muscleGroup = muscleGroup.name,
    isCustom = isCustom,
    isArchived = isArchived,
    description = description,
    wgerId = wgerId,
)
