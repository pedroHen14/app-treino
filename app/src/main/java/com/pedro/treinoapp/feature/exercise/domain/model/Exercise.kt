package com.pedro.treinoapp.feature.exercise.domain.model

data class Exercise(
    val id: Long = 0,
    val name: String,
    val muscleGroup: MuscleGroup,
    val isCustom: Boolean,
    val isArchived: Boolean = false,
)
