package com.pedro.treinoapp.feature.exercise.domain.model

data class Exercise(
    val id: Long = 0,
    val name: String,
    val muscleGroup: MuscleGroup,
    val isCustom: Boolean,
    val isArchived: Boolean = false,
    val description: String? = null,
    /** Id do exercício na API do wger, quando foi importado de lá. */
    val wgerId: Long? = null,
)
