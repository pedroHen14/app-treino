package com.pedro.treinoapp.feature.workout.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek

/** `templateId` nulo significa dia de descanso. */
@Entity(
    tableName = "weekly_schedule",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("template_id")],
)
data class WeeklyScheduleEntity(
    @PrimaryKey @ColumnInfo(name = "day_of_week") val dayOfWeek: DayOfWeek,
    @ColumnInfo(name = "template_id") val templateId: Long?,
)
