package com.pedro.treinoapp.feature.exercise.data.local

import com.pedro.treinoapp.feature.exercise.domain.model.Exercise
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup

fun ExerciseEntity.toDomain() = Exercise(
    id = id,
    name = name,
    muscleGroup = MuscleGroup.valueOf(muscleGroup),
    isCustom = isCustom,
    isArchived = isArchived,
)

/** O wgerId não existe no domínio ainda (PBI 7), então é preservado de quem chama. */
fun Exercise.toEntity(wgerId: Long? = null) = ExerciseEntity(
    id = id,
    name = name,
    muscleGroup = muscleGroup.name,
    isCustom = isCustom,
    isArchived = isArchived,
    wgerId = wgerId,
)
