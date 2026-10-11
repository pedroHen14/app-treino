package com.pedro.treinoapp.feature.workout.domain.model

import java.time.DayOfWeek

data class WorkoutTemplate(
    val id: Long = 0,
    val name: String,
    val exercises: List<TemplateExercise>,
)

/** Resumo para a lista de modelos. `scheduledDays` vem da agenda, em ordem de segunda a domingo. */
data class WorkoutTemplateSummary(
    val id: Long,
    val name: String,
    val exerciseCount: Int,
    val scheduledDays: List<DayOfWeek> = emptyList(),
)
