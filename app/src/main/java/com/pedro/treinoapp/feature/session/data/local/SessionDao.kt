package com.pedro.treinoapp.feature.session.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/** Uma linha por série; exercício sem séries aparece uma vez, com as colunas da série nulas. */
data class SessionSetRow(
    @ColumnInfo(name = "session_exercise_id") val sessionExerciseId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "exercise_name") val exerciseName: String,
    @ColumnInfo(name = "target_reps_min") val targetRepsMin: Int?,
    @ColumnInfo(name = "target_reps_max") val targetRepsMax: Int?,
    @ColumnInfo(name = "set_id") val setId: Long?,
    @ColumnInfo(name = "weight_grams") val weightGrams: Int?,
    @ColumnInfo(name = "reps") val reps: Int?,
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean?,
    @ColumnInfo(name = "completed_at") val completedAt: Instant?,
)

data class SessionDayRow(
    @ColumnInfo(name = "date") val date: LocalDate,
    @ColumnInfo(name = "template_id") val templateId: Long?,
    @ColumnInfo(name = "makeup_for_date") val makeupForDate: LocalDate?,
)

data class LastSetRow(
    @ColumnInfo(name = "weight_grams") val weightGrams: Int?,
    @ColumnInfo(name = "reps") val reps: Int?,
)

@Dao
abstract class SessionDao {

    @Query("SELECT * FROM workout_session WHERE finished_at IS NULL LIMIT 1")
    abstract fun observeActive(): Flow<WorkoutSessionEntity?>

    @Query("SELECT id FROM workout_session WHERE finished_at IS NULL LIMIT 1")
    abstract suspend fun getActiveId(): Long?

    @Query("SELECT * FROM workout_session WHERE id = :id")
    abstract fun observeById(id: Long): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_session WHERE id = :id")
    abstract suspend fun getById(id: Long): WorkoutSessionEntity?

    @Query(SESSION_SETS)
    abstract fun observeSets(sessionId: Long): Flow<List<SessionSetRow>>

    @Query(SESSION_SETS)
    abstract suspend fun getSets(sessionId: Long): List<SessionSetRow>

    @Query("SELECT template_id FROM workout_session WHERE date = :date AND finished_at IS NOT NULL")
    abstract fun observeFinishedTemplateIdsOn(date: LocalDate): Flow<List<Long?>>

    @Query("SELECT date, template_id, makeup_for_date FROM workout_session WHERE date BETWEEN :from AND :to")
    abstract suspend fun getSessionDays(from: LocalDate, to: LocalDate): List<SessionDayRow>

    @Query(
        "SELECT ws.weight_grams, ws.reps FROM workout_set ws " +
            "WHERE ws.is_completed = 1 AND ws.session_exercise_id = (" +
            "SELECT se.id FROM session_exercise se JOIN workout_session s ON s.id = se.session_id " +
            "WHERE se.exercise_id = :exerciseId AND s.finished_at IS NOT NULL " +
            "AND EXISTS (SELECT 1 FROM workout_set c WHERE c.session_exercise_id = se.id AND c.is_completed = 1) " +
            "ORDER BY s.date DESC, s.started_at DESC, se.position DESC LIMIT 1) " +
            "ORDER BY ws.position",
    )
    abstract suspend fun getLastCompletedSets(exerciseId: Long): List<LastSetRow>

    @Insert
    protected abstract suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Insert
    protected abstract suspend fun insertExercise(exercise: SessionExerciseEntity): Long

    @Insert
    protected abstract suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Insert
    protected abstract suspend fun insertSet(set: WorkoutSetEntity): Long

    @Query("UPDATE workout_set SET weight_grams = :weightGrams, reps = :reps WHERE id = :setId")
    abstract suspend fun updateSet(setId: Long, weightGrams: Int?, reps: Int?)

    @Query("UPDATE workout_set SET is_completed = :isCompleted, completed_at = :completedAt WHERE id = :setId")
    abstract suspend fun setCompleted(setId: Long, isCompleted: Boolean, completedAt: Instant?)

    @Query("DELETE FROM workout_set WHERE id = :setId")
    abstract suspend fun deleteSet(setId: Long)

    @Query("DELETE FROM session_exercise WHERE id = :sessionExerciseId")
    abstract suspend fun deleteExercise(sessionExerciseId: Long)

    @Query("DELETE FROM workout_session WHERE id = :sessionId")
    abstract suspend fun deleteSession(sessionId: Long)

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM workout_set WHERE session_exercise_id = :sessionExerciseId")
    protected abstract suspend fun nextSetPosition(sessionExerciseId: Long): Int

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM session_exercise WHERE session_id = :sessionId")
    protected abstract suspend fun nextExercisePosition(sessionId: Long): Int

    @Query("UPDATE session_exercise SET exercise_id = :exerciseId WHERE id = :sessionExerciseId")
    protected abstract suspend fun updateExerciseId(sessionExerciseId: Long, exerciseId: Long)

    @Query("DELETE FROM workout_set WHERE session_exercise_id = :sessionExerciseId")
    protected abstract suspend fun deleteSetsOf(sessionExerciseId: Long)

    @Query(
        "DELETE FROM workout_set WHERE is_completed = 0 AND session_exercise_id IN " +
            "(SELECT id FROM session_exercise WHERE session_id = :sessionId)",
    )
    protected abstract suspend fun deletePendingSets(sessionId: Long)

    @Query(
        "DELETE FROM session_exercise WHERE session_id = :sessionId AND NOT EXISTS " +
            "(SELECT 1 FROM workout_set ws WHERE ws.session_exercise_id = session_exercise.id)",
    )
    protected abstract suspend fun deleteEmptyExercises(sessionId: Long)

    @Query("UPDATE workout_session SET finished_at = :finishedAt WHERE id = :sessionId")
    protected abstract suspend fun setFinishedAt(sessionId: Long, finishedAt: Instant)

    @Query(
        "UPDATE workout_set SET is_completed = 1, completed_at = :completedAt " +
            "WHERE is_completed = 0 AND weight_grams IS NOT NULL AND reps IS NOT NULL AND session_exercise_id IN " +
            "(SELECT id FROM session_exercise WHERE session_id = :sessionId)",
    )
    protected abstract suspend fun completeFilledSets(sessionId: Long, completedAt: Instant)

    /** A posição de exercícios e séries é a ordem nas listas recebidas. */
    @Transaction
    open suspend fun insertWithExercises(
        session: WorkoutSessionEntity,
        exercises: List<Pair<SessionExerciseEntity, List<WorkoutSetEntity>>>,
    ): Long {
        val sessionId = insertSession(session)
        exercises.forEachIndexed { index, (exercise, sets) ->
            val exerciseId = insertExercise(exercise.copy(id = 0, sessionId = sessionId, position = index))
            insertSets(sets.withPositions(exerciseId))
        }
        return sessionId
    }

    @Transaction
    open suspend fun appendExercise(exercise: SessionExerciseEntity, sets: List<WorkoutSetEntity>): Long {
        val position = nextExercisePosition(exercise.sessionId)
        val id = insertExercise(exercise.copy(id = 0, position = position))
        insertSets(sets.withPositions(id))
        return id
    }

    @Transaction
    open suspend fun appendSet(set: WorkoutSetEntity): Long =
        insertSet(set.copy(id = 0, position = nextSetPosition(set.sessionExerciseId)))

    @Transaction
    open suspend fun replaceExercise(sessionExerciseId: Long, exerciseId: Long, sets: List<WorkoutSetEntity>) {
        updateExerciseId(sessionExerciseId, exerciseId)
        deleteSetsOf(sessionExerciseId)
        insertSets(sets.withPositions(sessionExerciseId))
    }

    @Transaction
    open suspend fun finish(sessionId: Long, finishedAt: Instant) {
        deletePendingSets(sessionId)
        deleteEmptyExercises(sessionId)
        setFinishedAt(sessionId, finishedAt)
    }

    @Transaction
    open suspend fun saveEdit(sessionId: Long, completedAt: Instant) {
        completeFilledSets(sessionId, completedAt)
        deletePendingSets(sessionId)
        deleteEmptyExercises(sessionId)
    }

    private fun List<WorkoutSetEntity>.withPositions(sessionExerciseId: Long) =
        mapIndexed { index, it -> it.copy(id = 0, sessionExerciseId = sessionExerciseId, position = index) }

    private companion object {
        const val SESSION_SETS =
            "SELECT se.id AS session_exercise_id, se.exercise_id, e.name AS exercise_name, " +
                "se.target_reps_min, se.target_reps_max, ws.id AS set_id, ws.weight_grams, ws.reps, " +
                "ws.is_completed, ws.completed_at FROM session_exercise se " +
                "JOIN exercise e ON e.id = se.exercise_id " +
                "LEFT JOIN workout_set ws ON ws.session_exercise_id = se.id " +
                "WHERE se.session_id = :sessionId ORDER BY se.position, ws.position"
    }
}
