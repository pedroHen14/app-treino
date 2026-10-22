package com.pedro.treinoapp.feature.session.data

import com.pedro.treinoapp.feature.session.data.local.SessionDao
import com.pedro.treinoapp.feature.session.data.local.toDomain
import com.pedro.treinoapp.feature.session.data.local.toEntity
import com.pedro.treinoapp.feature.session.domain.model.NewSessionExercise
import com.pedro.treinoapp.feature.session.domain.model.NewWorkoutSession
import com.pedro.treinoapp.feature.session.domain.model.SessionDay
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSession
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSet
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class SessionRepositoryImpl @Inject constructor(
    private val dao: SessionDao,
) : SessionRepository {

    override fun observeActive(): Flow<WorkoutSession?> =
        dao.observeActive()
            .map { it?.id }
            .distinctUntilChanged()
            .flatMapLatest { id -> if (id == null) flowOf(null) else observe(id) }

    override suspend fun getActiveId(): Long? = dao.getActiveId()

    override fun observe(id: Long): Flow<WorkoutSession?> =
        combine(dao.observeById(id), dao.observeSets(id)) { session, rows -> session?.toDomain(rows) }

    override suspend fun get(id: Long): WorkoutSession? = dao.getById(id)?.toDomain(dao.getSets(id))

    override fun observeFinishedTemplateIdsOn(date: LocalDate): Flow<List<Long?>> =
        dao.observeFinishedTemplateIdsOn(date)

    override fun observeAdvanceDateFor(date: LocalDate): Flow<LocalDate?> = dao.observeAdvanceDateFor(date)

    override suspend fun getSessionDays(from: LocalDate, to: LocalDate): List<SessionDay> =
        dao.getSessionDays(from, to).map { it.toDomain() }

    override fun observeFinishedSessionDays(from: LocalDate, to: LocalDate): Flow<List<SessionDay>> =
        dao.observeFinishedSessionDays(from, to).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getLastCompletedSets(exerciseId: Long): List<WorkoutSet> =
        dao.getLastCompletedSets(exerciseId).map { it.toDomain() }

    override suspend fun create(session: NewWorkoutSession): Long =
        dao.insertWithExercises(
            session.toEntity(),
            session.exercises.map { exercise -> exercise.toEntity() to exercise.sets.map { it.toEntity() } },
        )

    override suspend fun updateSet(setId: Long, weightGrams: Int?, reps: Int?) =
        dao.updateSet(setId, weightGrams, reps)

    override suspend fun setCompleted(setId: Long, completedAt: Instant?) =
        dao.setCompleted(setId, isCompleted = completedAt != null, completedAt = completedAt)

    override suspend fun addSet(sessionExerciseId: Long, set: WorkoutSet): Long =
        dao.appendSet(set.toEntity(sessionExerciseId))

    override suspend fun removeSet(setId: Long) = dao.deleteSet(setId)

    override suspend fun addExercise(sessionId: Long, exercise: NewSessionExercise): Long =
        dao.appendExercise(exercise.toEntity(sessionId), exercise.sets.map { it.toEntity() })

    override suspend fun removeExercise(sessionExerciseId: Long) = dao.deleteExercise(sessionExerciseId)

    override suspend fun replaceExercise(sessionExerciseId: Long, exerciseId: Long, sets: List<WorkoutSet>) =
        dao.replaceExercise(sessionExerciseId, exerciseId, sets.map { it.toEntity() })

    override suspend fun finish(sessionId: Long, finishedAt: Instant) = dao.finish(sessionId, finishedAt)

    override suspend fun saveEdit(sessionId: Long, completedAt: Instant) = dao.saveEdit(sessionId, completedAt)

    override suspend fun setMakeupForDate(sessionId: Long, date: LocalDate?) = dao.updateMakeupForDate(sessionId, date)

    override suspend fun delete(sessionId: Long) = dao.deleteSession(sessionId)
}
