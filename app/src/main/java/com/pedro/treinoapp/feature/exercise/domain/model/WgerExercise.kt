package com.pedro.treinoapp.feature.exercise.domain.model

/** Resultado da busca no wger, ainda não salvo no catálogo. */
data class WgerExercise(
    val wgerId: Long,
    val name: String,
    /** Falso quando o wger não tem tradução em português e o nome veio em inglês. */
    val isNameInPortuguese: Boolean,
    val description: String?,
    /** Nulo quando os músculos e a categoria do wger não correspondem a nenhum grupo do app. */
    val muscleGroup: MuscleGroup?,
)
