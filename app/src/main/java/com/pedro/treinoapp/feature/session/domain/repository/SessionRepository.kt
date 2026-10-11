package com.pedro.treinoapp.feature.session.domain.repository

import com.pedro.treinoapp.feature.session.domain.model.NewSessionExercise
import com.pedro.treinoapp.feature.session.domain.model.NewWorkoutSession
import com.pedro.treinoapp.feature.session.domain.model.SessionDay
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSession
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSet
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

/**
 * Sessões vêm com exercícios e séries na ordem de execução. Novos exercícios e séries
 * entram no fim da lista.
 */
interface SessionRepository {
    fun observeActive(): Flow<WorkoutSession?>
    suspend fun getActiveId(): Long?
    fun observe(id: Long): Flow<WorkoutSession?>
    suspend fun get(id: Long): WorkoutSession?

    /** Templates (ids) das sessões finalizadas no dia. */
    fun observeFinishedTemplateIdsOn(date: LocalDate): Flow<List<Long?>>

    /** Sessões com data entre [from] e [to], inclusive. */
    suspend fun getSessionDays(from: LocalDate, to: LocalDate): List<SessionDay>

    /** Séries concluídas do exercício na sessão finalizada mais recente que o tem; vazia se nunca foi feito. */
    suspend fun getLastCompletedSets(exerciseId: Long): List<WorkoutSet>

    suspend fun create(session: NewWorkoutSession): Long
    suspend fun updateSet(setId: Long, weightGrams: Int?, reps: Int?)
    suspend fun setCompleted(setId: Long, completedAt: Instant?)
    suspend fun addSet(sessionExerciseId: Long, set: WorkoutSet): Long
    suspend fun removeSet(setId: Long)
    suspend fun addExercise(sessionId: Long, exercise: NewSessionExercise): Long
    suspend fun removeExercise(sessionExerciseId: Long)

    /** Troca o exercício mantendo posição e alvo; as séries são substituídas por [sets]. */
    suspend fun replaceExercise(sessionExerciseId: Long, exerciseId: Long, sets: List<WorkoutSet>)

    /** Descarta séries não concluídas e exercícios que ficarem sem séries, e marca a sessão como finalizada. */
    suspend fun finish(sessionId: Long, finishedAt: Instant)
    suspend fun delete(sessionId: Long)
}
