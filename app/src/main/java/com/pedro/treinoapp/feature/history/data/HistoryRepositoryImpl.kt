package com.pedro.treinoapp.feature.history.data

import com.pedro.treinoapp.feature.history.data.local.HistoryDao
import com.pedro.treinoapp.feature.history.data.local.toDomain
import com.pedro.treinoapp.feature.history.domain.model.ExerciseWithHistory
import com.pedro.treinoapp.feature.history.domain.model.FinishedSession
import com.pedro.treinoapp.feature.history.domain.model.RecordedSet
import com.pedro.treinoapp.feature.history.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HistoryRepositoryImpl @Inject constructor(
    private val dao: HistoryDao,
) : HistoryRepository {

    override fun observeFinishedSessions(): Flow<List<FinishedSession>> =
        dao.observeFinishedSessions().map { rows -> rows.map { it.toDomain() } }

    override fun observeCompletedSets(): Flow<List<RecordedSet>> =
        dao.observeCompletedSets().map { rows -> rows.map { it.toDomain() } }

    override fun observeCompletedSets(exerciseIds: List<Long>, includeSessionId: Long?): Flow<List<RecordedSet>> =
        dao.observeCompletedSets(exerciseIds, includeSessionId ?: NO_SESSION).map { rows -> rows.map { it.toDomain() } }

    override fun observeExercisesWithHistory(): Flow<List<ExerciseWithHistory>> =
        dao.observeExercisesWithHistory().map { rows -> rows.map { it.toDomain() } }

    override fun observeSessionExerciseIds(sessionId: Long): Flow<List<Long>> =
        dao.observeSessionExerciseIds(sessionId)

    private companion object {
        const val NO_SESSION = -1L
    }
}
