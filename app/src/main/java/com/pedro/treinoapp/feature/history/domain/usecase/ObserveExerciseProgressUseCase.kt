package com.pedro.treinoapp.feature.history.domain.usecase

import com.pedro.treinoapp.feature.history.domain.PersonalRecordCalculator
import com.pedro.treinoapp.feature.history.domain.model.ExerciseExecution
import com.pedro.treinoapp.feature.history.domain.model.ExerciseProgress
import com.pedro.treinoapp.feature.history.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Execuções do exercício em sessões finalizadas, da mais recente para a mais antiga, e os recordes vigentes. */
class ObserveExerciseProgressUseCase @Inject constructor(
    private val repository: HistoryRepository,
) {
    operator fun invoke(exerciseId: Long): Flow<ExerciseProgress> =
        repository.observeCompletedSets(listOf(exerciseId)).map { sets ->
            val analysis = PersonalRecordCalculator.analyze(sets)
            val executions = sets.groupBy { it.sessionId }.map { (sessionId, sessionSets) ->
                ExerciseExecution(sessionId = sessionId, date = sessionSets.first().date, sets = sessionSets)
            }
            ExerciseProgress(
                executions = executions.reversed(),
                records = analysis.current,
                recordsBySet = analysis.recordsBySet,
            )
        }
}
