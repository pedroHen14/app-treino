package com.pedro.treinoapp.feature.history.domain

import com.pedro.treinoapp.feature.history.domain.model.ExerciseWithHistory
import com.pedro.treinoapp.feature.history.domain.model.FinishedSession
import com.pedro.treinoapp.feature.history.domain.model.RecordedSet
import com.pedro.treinoapp.feature.history.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Em memória. [sets] já deve estar em ordem cronológica; séries de sessões fora de [sessions]
 * são de uma sessão em andamento.
 */
class FakeHistoryRepository(
    sessions: List<FinishedSession> = emptyList(),
    sets: List<RecordedSet> = emptyList(),
) : HistoryRepository {

    private val sessions = MutableStateFlow(sessions)
    private val sets = MutableStateFlow(sets)

    /** Como o banco faz ao excluir: a sessão sai e as séries vão junto em cascata. */
    fun deleteSession(id: Long) {
        sessions.update { list -> list.filterNot { it.id == id } }
        sets.update { list -> list.filterNot { it.sessionId == id } }
    }

    fun updateSet(setId: Long, weightGrams: Int, reps: Int) =
        sets.update { list -> list.map { if (it.setId == setId) it.copy(weightGrams = weightGrams, reps = reps) else it } }

    override fun observeFinishedSessions(): Flow<List<FinishedSession>> =
        sessions.map { list -> list.sortedWith(compareByDescending<FinishedSession> { it.date }.thenByDescending { it.startedAt }) }

    override fun observeCompletedSets(): Flow<List<RecordedSet>> =
        combine(sessions, sets) { sessions, sets ->
            val finished = sessions.map { it.id }.toSet()
            sets.filter { it.sessionId in finished }
        }

    override fun observeCompletedSets(exerciseIds: List<Long>, includeSessionId: Long?): Flow<List<RecordedSet>> =
        combine(sessions, sets) { sessions, sets ->
            val finished = sessions.map { it.id }.toSet()
            sets.filter { it.exerciseId in exerciseIds && (it.sessionId in finished || it.sessionId == includeSessionId) }
        }

    override fun observeExercisesWithHistory(): Flow<List<ExerciseWithHistory>> =
        observeCompletedSets().map { sets ->
            sets.groupBy { it.exerciseId }
                .map { (exerciseId, list) ->
                    ExerciseWithHistory(exerciseId, "Exercício $exerciseId", list.maxOf { it.date }, list.map { it.sessionId }.distinct().size)
                }
                .sortedByDescending { it.lastDate }
        }

    override fun observeSessionExerciseIds(sessionId: Long): Flow<List<Long>> =
        sets.map { list -> list.filter { it.sessionId == sessionId }.map { it.exerciseId }.distinct() }
}
