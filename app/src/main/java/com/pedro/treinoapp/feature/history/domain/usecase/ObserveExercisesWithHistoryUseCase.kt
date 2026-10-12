package com.pedro.treinoapp.feature.history.domain.usecase

import com.pedro.treinoapp.feature.history.domain.model.ExerciseWithHistory
import com.pedro.treinoapp.feature.history.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveExercisesWithHistoryUseCase @Inject constructor(
    private val repository: HistoryRepository,
) {
    operator fun invoke(): Flow<List<ExerciseWithHistory>> = repository.observeExercisesWithHistory()
}
