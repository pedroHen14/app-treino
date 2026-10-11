package com.pedro.treinoapp.feature.workout.data

import com.pedro.treinoapp.feature.workout.data.local.WorkoutTemplateDao
import com.pedro.treinoapp.feature.workout.data.local.WorkoutTemplateEntity
import com.pedro.treinoapp.feature.workout.data.local.toDomain
import com.pedro.treinoapp.feature.workout.data.local.toEntity
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutTemplate
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutTemplateSummary
import com.pedro.treinoapp.feature.workout.domain.repository.WorkoutTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

class WorkoutTemplateRepositoryImpl @Inject constructor(
    private val dao: WorkoutTemplateDao,
    private val clock: Clock,
) : WorkoutTemplateRepository {

    override fun observeSummaries(): Flow<List<WorkoutTemplateSummary>> =
        dao.observeSummaries().map { list -> list.map { it.toDomain() } }

    override suspend fun getSummaries(): List<WorkoutTemplateSummary> = dao.getSummaries().map { it.toDomain() }

    override suspend fun getById(id: Long): WorkoutTemplate? =
        dao.getById(id)?.toDomain(dao.getExercises(id))

    override suspend fun insert(template: WorkoutTemplate): Long {
        val now = clock.instant()
        val entity = WorkoutTemplateEntity(name = template.name, createdAt = now, updatedAt = now)
        return dao.insertWithExercises(entity, template.exercises.map { it.toEntity() })
    }

    override suspend fun update(template: WorkoutTemplate) =
        dao.replace(template.id, template.name, clock.instant(), template.exercises.map { it.toEntity() })

    override suspend fun delete(id: Long) = dao.deleteById(id)
}
