package com.pedro.treinoapp.feature.workout.domain

import com.pedro.treinoapp.feature.workout.domain.model.WorkoutTemplate
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutTemplateSummary
import com.pedro.treinoapp.feature.workout.domain.repository.WorkoutTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeWorkoutTemplateRepository(initial: List<WorkoutTemplate> = emptyList()) : WorkoutTemplateRepository {

    private val templates = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    val current: List<WorkoutTemplate> get() = templates.value

    override fun observeSummaries(): Flow<List<WorkoutTemplateSummary>> =
        templates.map { list -> list.map { it.toSummary() } }

    override suspend fun getSummaries(): List<WorkoutTemplateSummary> = templates.value.map { it.toSummary() }

    override suspend fun getById(id: Long): WorkoutTemplate? = templates.value.find { it.id == id }

    override suspend fun insert(template: WorkoutTemplate): Long {
        val id = nextId++
        templates.update { it + template.copy(id = id) }
        return id
    }

    override suspend fun update(template: WorkoutTemplate) {
        templates.update { list -> list.map { if (it.id == template.id) template else it } }
    }

    override suspend fun delete(id: Long) {
        templates.update { list -> list.filterNot { it.id == id } }
    }

    private fun WorkoutTemplate.toSummary() = WorkoutTemplateSummary(id, name, exercises.size)
}
