package com.pedro.treinoapp.feature.exercise.domain.model

data class ExerciseFilter(
    val query: String = "",
    val muscleGroup: MuscleGroup? = null,
    val includeArchived: Boolean = false,
)
