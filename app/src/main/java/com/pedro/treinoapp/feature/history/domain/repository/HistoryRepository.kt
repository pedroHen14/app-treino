package com.pedro.treinoapp.feature.history.domain.repository

import com.pedro.treinoapp.feature.history.domain.model.ExerciseWithHistory
import com.pedro.treinoapp.feature.history.domain.model.FinishedSession
import com.pedro.treinoapp.feature.history.domain.model.RecordedSet
import kotlinx.coroutines.flow.Flow

/**
 * Leitura do que foi executado. As séries vêm só concluídas e em ordem cronológica: data da sessão,
 * início, posição do exercício e da série, que é a ordem que o cálculo de recordes espera.
 */
interface HistoryRepository {

    /** Sessões finalizadas, da mais recente para a mais antiga. */
    fun observeFinishedSessions(): Flow<List<FinishedSession>>

    /** Séries concluídas de todos os exercícios em sessões finalizadas. */
    fun observeCompletedSets(): Flow<List<RecordedSet>>

    /**
     * Séries concluídas dos exercícios em sessões finalizadas e também na sessão [includeSessionId],
     * mesmo que ainda esteja em andamento.
     */
    fun observeCompletedSets(exerciseIds: List<Long>, includeSessionId: Long? = null): Flow<List<RecordedSet>>

    /** Exercícios com histórico, do feito mais recentemente para o mais antigo. */
    fun observeExercisesWithHistory(): Flow<List<ExerciseWithHistory>>

    /** Exercícios da sessão, na ordem dela. */
    fun observeSessionExerciseIds(sessionId: Long): Flow<List<Long>>
}
