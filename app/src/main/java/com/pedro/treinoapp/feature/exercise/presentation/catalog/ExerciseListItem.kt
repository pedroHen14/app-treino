package com.pedro.treinoapp.feature.exercise.presentation.catalog

import com.pedro.treinoapp.feature.exercise.domain.model.Exercise

/** `isSelected` só é usado no modo seleção do catálogo. */
data class ExerciseListItem(
    val exercise: Exercise,
    val isSelected: Boolean = false,
)
