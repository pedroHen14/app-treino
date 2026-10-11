package com.pedro.treinoapp.feature.workout.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.DayOfWeek

@Dao
interface WeeklyScheduleDao {

    @Query("SELECT * FROM weekly_schedule")
    fun observeAll(): Flow<List<WeeklyScheduleEntity>>

    @Query("SELECT day_of_week FROM weekly_schedule WHERE template_id = :templateId ORDER BY day_of_week")
    suspend fun getDaysFor(templateId: Long): List<DayOfWeek>

    @Upsert
    suspend fun upsert(day: WeeklyScheduleEntity)

    @Query("UPDATE weekly_schedule SET template_id = NULL WHERE template_id = :templateId")
    suspend fun clearTemplate(templateId: Long)
}
