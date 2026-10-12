package com.pedro.treinoapp.feature.exercise.data

import com.pedro.treinoapp.feature.exercise.data.local.ExerciseDao
import com.pedro.treinoapp.feature.exercise.data.local.toDomain
import com.pedro.treinoapp.feature.exercise.data.local.toEntity
import com.pedro.treinoapp.feature.exercise.domain.model.Exercise
import com.pedro.treinoapp.feature.exercise.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ExerciseRepositoryImpl @Inject constructor(
    private val dao: ExerciseDao,
) : ExerciseRepository {

    override fun observeAll(): Flow<List<Exercise>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<Exercise> = dao.getAll().map { it.toDomain() }

    override suspend fun getById(id: Long): Exercise? = dao.getById(id)?.toDomain()

    override suspend fun insert(exercise: Exercise): Long = dao.insert(exercise.toEntity())

    override suspend fun update(exercise: Exercise) = dao.update(exercise.toEntity())

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override suspend fun isUsedInSession(id: Long): Boolean = dao.isUsedInSession(id)

    override suspend fun isUsedInTemplate(id: Long): Boolean = dao.isUsedInTemplate(id)
}
