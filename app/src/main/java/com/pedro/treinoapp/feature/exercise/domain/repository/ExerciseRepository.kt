package com.pedro.treinoapp.feature.exercise.domain.repository

import com.pedro.treinoapp.feature.exercise.domain.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun observeAll(): Flow<List<Exercise>>
    suspend fun getAll(): List<Exercise>
    suspend fun getById(id: Long): Exercise?
    suspend fun insert(exercise: Exercise): Long
    suspend fun update(exercise: Exercise)
    suspend fun delete(id: Long)
    suspend fun isUsedInSession(id: Long): Boolean
    suspend fun isUsedInTemplate(id: Long): Boolean
}
