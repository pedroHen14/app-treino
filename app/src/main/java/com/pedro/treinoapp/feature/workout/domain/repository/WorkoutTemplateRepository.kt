package com.pedro.treinoapp.feature.workout.domain.repository

import com.pedro.treinoapp.feature.workout.domain.model.WorkoutTemplate
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutTemplateSummary
import kotlinx.coroutines.flow.Flow

/** Os resumos vêm sem `scheduledDays`; quem cruza com a agenda são os casos de uso. */
interface WorkoutTemplateRepository {
    fun observeSummaries(): Flow<List<WorkoutTemplateSummary>>
    suspend fun getSummaries(): List<WorkoutTemplateSummary>
    suspend fun getById(id: Long): WorkoutTemplate?
    suspend fun insert(template: WorkoutTemplate): Long

    /** Troca nome e lista de exercícios. Sessões já registradas não são tocadas. */
    suspend fun update(template: WorkoutTemplate)
    suspend fun delete(id: Long)
}
