package com.pedro.treinoapp.feature.workout.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.Instant

data class TemplateSummaryRow(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "exercise_count") val exerciseCount: Int,
)

data class TemplateExerciseRow(
    @Embedded val entity: TemplateExerciseEntity,
    @ColumnInfo(name = "exercise_name") val exerciseName: String,
)

@Dao
abstract class WorkoutTemplateDao {

    @Query(SUMMARIES)
    abstract fun observeSummaries(): Flow<List<TemplateSummaryRow>>

    @Query(SUMMARIES)
    abstract suspend fun getSummaries(): List<TemplateSummaryRow>

    @Query("SELECT * FROM workout_template WHERE id = :id")
    abstract suspend fun getById(id: Long): WorkoutTemplateEntity?

    @Query(
        "SELECT te.*, e.name AS exercise_name FROM template_exercise te " +
            "JOIN exercise e ON e.id = te.exercise_id " +
            "WHERE te.template_id = :templateId ORDER BY te.position",
    )
    abstract suspend fun getExercises(templateId: Long): List<TemplateExerciseRow>

    @Insert
    protected abstract suspend fun insertTemplate(template: WorkoutTemplateEntity): Long

    @Insert
    protected abstract suspend fun insertExercises(exercises: List<TemplateExerciseEntity>)

    @Query("UPDATE workout_template SET name = :name, updated_at = :updatedAt WHERE id = :id")
    protected abstract suspend fun updateTemplate(id: Long, name: String, updatedAt: Instant)

    @Query("DELETE FROM template_exercise WHERE template_id = :templateId")
    protected abstract suspend fun deleteExercises(templateId: Long)

    @Query("DELETE FROM workout_template WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    /** A posição de cada exercício é a ordem na lista recebida. */
    @Transaction
    open suspend fun insertWithExercises(
        template: WorkoutTemplateEntity,
        exercises: List<TemplateExerciseEntity>,
    ): Long {
        val id = insertTemplate(template)
        insertExercises(exercises.withPositions(id))
        return id
    }

    /** Troca nome e exercícios do modelo. Sessões guardam seus próprios exercícios e não mudam. */
    @Transaction
    open suspend fun replace(
        id: Long,
        name: String,
        updatedAt: Instant,
        exercises: List<TemplateExerciseEntity>,
    ) {
        updateTemplate(id, name, updatedAt)
        deleteExercises(id)
        insertExercises(exercises.withPositions(id))
    }

    private fun List<TemplateExerciseEntity>.withPositions(templateId: Long) =
        mapIndexed { index, it -> it.copy(id = 0, templateId = templateId, position = index) }

    private companion object {
        const val SUMMARIES =
            "SELECT t.id, t.name, COUNT(te.id) AS exercise_count FROM workout_template t " +
                "LEFT JOIN template_exercise te ON te.template_id = t.id GROUP BY t.id"
    }
}
