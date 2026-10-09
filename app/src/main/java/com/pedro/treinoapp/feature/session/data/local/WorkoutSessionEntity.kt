package com.pedro.treinoapp.feature.session.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pedro.treinoapp.feature.workout.data.local.WorkoutTemplateEntity
import java.time.Instant
import java.time.LocalDate

/**
 * `finishedAt` nulo indica sessão em andamento. `makeupForDate` guarda o dia reposto,
 * quando a sessão é uma reposição.
 */
@Entity(
    tableName = "workout_session",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["template_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("template_id"), Index("date")],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "template_id") val templateId: Long?,
    @ColumnInfo(name = "template_name") val templateName: String?,
    @ColumnInfo(name = "date") val date: LocalDate,
    @ColumnInfo(name = "started_at") val startedAt: Instant,
    @ColumnInfo(name = "finished_at") val finishedAt: Instant? = null,
    @ColumnInfo(name = "makeup_for_date") val makeupForDate: LocalDate? = null,
    @ColumnInfo(name = "notes") val notes: String? = null,
)
