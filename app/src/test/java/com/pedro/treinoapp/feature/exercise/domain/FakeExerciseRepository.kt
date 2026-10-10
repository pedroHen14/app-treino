package com.pedro.treinoapp.feature.exercise.domain

import com.pedro.treinoapp.feature.exercise.domain.model.Exercise
import com.pedro.treinoapp.feature.exercise.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeExerciseRepository(initial: List<Exercise> = emptyList()) : ExerciseRepository {

    private val exercises = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    val usedInSession = mutableSetOf<Long>()
    val usedInTemplate = mutableSetOf<Long>()

    val current: List<Exercise> get() = exercises.value

    override fun observeAll(): Flow<List<Exercise>> = exercises

    override suspend fun getAll(): List<Exercise> = exercises.value

    override suspend fun getById(id: Long): Exercise? = exercises.value.find { it.id == id }

    override suspend fun insert(exercise: Exercise): Long {
        val id = nextId++
        exercises.update { it + exercise.copy(id = id) }
        return id
    }

    override suspend fun update(exercise: Exercise) {
        exercises.update { list -> list.map { if (it.id == exercise.id) exercise else it } }
    }

    override suspend fun delete(id: Long) {
        exercises.update { list -> list.filterNot { it.id == id } }
    }

    override suspend fun isUsedInSession(id: Long): Boolean = id in usedInSession

    override suspend fun isUsedInTemplate(id: Long): Boolean = id in usedInTemplate
}
