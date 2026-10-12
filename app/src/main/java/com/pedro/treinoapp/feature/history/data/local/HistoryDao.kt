package com.pedro.treinoapp.feature.history.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

data class FinishedSessionRow(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "template_name") val templateName: String?,
    @ColumnInfo(name = "date") val date: LocalDate,
    @ColumnInfo(name = "started_at") val startedAt: Instant,
    @ColumnInfo(name = "finished_at") val finishedAt: Instant,
    @ColumnInfo(name = "makeup_for_date") val makeupForDate: LocalDate?,
    @ColumnInfo(name = "exercise_count") val exerciseCount: Int,
)

data class CompletedSetRow(
    @ColumnInfo(name = "set_id") val setId: Long,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "date") val date: LocalDate,
    @ColumnInfo(name = "weight_grams") val weightGrams: Int,
    @ColumnInfo(name = "reps") val reps: Int,
)

data class ExerciseHistoryRow(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "last_date") val lastDate: LocalDate,
    @ColumnInfo(name = "session_count") val sessionCount: Int,
)

/** Só consultas: o histórico lê as tabelas de sessão, quem grava é a feature de sessão. */
@Dao
abstract class HistoryDao {

    @Query(
        "SELECT s.id, s.template_name, s.date, s.started_at, s.finished_at, s.makeup_for_date, " +
            "(SELECT COUNT(*) FROM session_exercise se WHERE se.session_id = s.id) AS exercise_count " +
            "FROM workout_session s WHERE s.finished_at IS NOT NULL " +
            "ORDER BY s.date DESC, s.started_at DESC, s.id DESC",
    )
    abstract fun observeFinishedSessions(): Flow<List<FinishedSessionRow>>

    @Query("$COMPLETED_SETS WHERE $COMPLETED_IN_FINISHED $CHRONOLOGICAL")
    abstract fun observeCompletedSets(): Flow<List<CompletedSetRow>>

    /** [includeSessionId] entra mesmo em andamento; -1 quando não há sessão a incluir. */
    @Query(
        "$COMPLETED_SETS WHERE ws.is_completed = 1 AND se.exercise_id IN (:exerciseIds) " +
            "AND (s.finished_at IS NOT NULL OR s.id = :includeSessionId) $CHRONOLOGICAL",
    )
    abstract fun observeCompletedSets(exerciseIds: List<Long>, includeSessionId: Long): Flow<List<CompletedSetRow>>

    @Query(
        "SELECT e.id, e.name, MAX(s.date) AS last_date, COUNT(DISTINCT s.id) AS session_count " +
            "FROM workout_set ws JOIN session_exercise se ON se.id = ws.session_exercise_id " +
            "JOIN workout_session s ON s.id = se.session_id JOIN exercise e ON e.id = se.exercise_id " +
            "WHERE $COMPLETED_IN_FINISHED GROUP BY e.id ORDER BY last_date DESC, e.name",
    )
    abstract fun observeExercisesWithHistory(): Flow<List<ExerciseHistoryRow>>

    @Query("SELECT exercise_id FROM session_exercise WHERE session_id = :sessionId ORDER BY position")
    abstract fun observeSessionExerciseIds(sessionId: Long): Flow<List<Long>>

    private companion object {
        const val COMPLETED_SETS =
            "SELECT ws.id AS set_id, s.id AS session_id, se.exercise_id, s.date, ws.weight_grams, ws.reps " +
                "FROM workout_set ws JOIN session_exercise se ON se.id = ws.session_exercise_id " +
                "JOIN workout_session s ON s.id = se.session_id"
        const val COMPLETED_IN_FINISHED = "ws.is_completed = 1 AND s.finished_at IS NOT NULL"
        const val CHRONOLOGICAL = "ORDER BY s.date, s.started_at, s.id, se.position, ws.position"
    }
}
