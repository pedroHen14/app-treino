package com.pedro.treinoapp.feature.history.domain.usecase

import com.pedro.treinoapp.feature.history.domain.PersonalRecordCalculator
import com.pedro.treinoapp.feature.history.domain.model.RecordType
import com.pedro.treinoapp.feature.history.domain.repository.HistoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Recordes batidos nas séries da sessão (em andamento ou finalizada), por id da série. Cada série é
 * comparada com tudo o que veio antes dela no histórico do exercício, então o resultado acompanha
 * conclusões, edições e exclusões.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObserveSessionRecordsUseCase @Inject constructor(
    private val repository: HistoryRepository,
) {
    operator fun invoke(sessionId: Long): Flow<Map<Long, Set<RecordType>>> =
        repository.observeSessionExerciseIds(sessionId)
            .map { it.distinct() }
            .distinctUntilChanged()
            .flatMapLatest { exerciseIds ->
                if (exerciseIds.isEmpty()) {
                    flowOf(emptyMap())
                } else {
                    repository.observeCompletedSets(exerciseIds, includeSessionId = sessionId).map { sets ->
                        val sessionSetIds = sets.filter { it.sessionId == sessionId }.map { it.setId }.toSet()
                        sets.groupBy { it.exerciseId }.values
                            .flatMap { PersonalRecordCalculator.analyze(it).recordsBySet.entries }
                            .filter { it.key in sessionSetIds }
                            .associate { it.key to it.value }
                    }
                }
            }
}
