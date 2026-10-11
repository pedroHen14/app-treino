package com.pedro.treinoapp.feature.workout.data

import com.pedro.treinoapp.feature.workout.data.local.WeeklyScheduleDao
import com.pedro.treinoapp.feature.workout.data.local.WeeklyScheduleEntity
import com.pedro.treinoapp.feature.workout.domain.repository.WeeklyScheduleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import javax.inject.Inject

class WeeklyScheduleRepositoryImpl @Inject constructor(
    private val dao: WeeklyScheduleDao,
) : WeeklyScheduleRepository {

    override fun observe(): Flow<Map<DayOfWeek, Long?>> =
        dao.observeAll().map { rows -> rows.associate { it.dayOfWeek to it.templateId } }

    override suspend fun getDaysFor(templateId: Long): List<DayOfWeek> = dao.getDaysFor(templateId)

    override suspend fun setDay(dayOfWeek: DayOfWeek, templateId: Long?) =
        dao.upsert(WeeklyScheduleEntity(dayOfWeek, templateId))

    override suspend fun clearTemplate(templateId: Long) = dao.clearTemplate(templateId)
}
