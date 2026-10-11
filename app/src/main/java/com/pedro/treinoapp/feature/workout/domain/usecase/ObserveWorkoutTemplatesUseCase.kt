package com.pedro.treinoapp.feature.workout.domain.usecase

import com.pedro.treinoapp.feature.workout.domain.model.WorkoutTemplateSummary
import com.pedro.treinoapp.feature.workout.domain.repository.WeeklyScheduleRepository
import com.pedro.treinoapp.feature.workout.domain.repository.WorkoutTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

/** Modelos em ordem alfabética do português, cada um com os dias em que está na agenda. */
class ObserveWorkoutTemplatesUseCase @Inject constructor(
    private val templates: WorkoutTemplateRepository,
    private val schedule: WeeklyScheduleRepository,
) {
    private val collator = Collator.getInstance(Locale.forLanguageTag("pt-BR"))

    operator fun invoke(): Flow<List<WorkoutTemplateSummary>> =
        combine(templates.observeSummaries(), schedule.observe()) { summaries, days ->
            summaries
                .map { summary ->
                    val scheduled = days.filterValues { it == summary.id }.keys.sorted()
                    summary.copy(scheduledDays = scheduled)
                }
                .sortedWith(compareBy(collator) { it.name })
        }
}
