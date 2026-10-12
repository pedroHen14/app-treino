package com.pedro.treinoapp.feature.history.presentation.progress

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedro.treinoapp.feature.exercise.domain.usecase.GetExerciseUseCase
import com.pedro.treinoapp.feature.history.domain.model.ExerciseExecution
import com.pedro.treinoapp.feature.history.domain.model.PersonalRecords
import com.pedro.treinoapp.feature.history.domain.model.ProgressPoint
import com.pedro.treinoapp.feature.history.domain.usecase.ObserveExerciseProgressUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class ProgressMetric { MAX_WEIGHT, VOLUME }

/** Execução na lista: as séries e se alguma delas bateu recorde. */
data class ExecutionItem(val execution: ExerciseExecution, val hasRecord: Boolean)

data class ExerciseProgressUiState(
    val isLoading: Boolean = true,
    val exerciseName: String? = null,
    val metric: ProgressMetric = ProgressMetric.MAX_WEIGHT,
    val points: List<ProgressPoint> = emptyList(),
    val records: PersonalRecords = PersonalRecords(maxWeight = null, bestRepsByWeight = emptyList()),
    val executions: List<ExecutionItem> = emptyList(),
)

@HiltViewModel
class ExerciseProgressViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    getExercise: GetExerciseUseCase,
    observeProgress: ObserveExerciseProgressUseCase,
) : ViewModel() {

    private val exerciseId: Long = checkNotNull(savedStateHandle[ExerciseProgressFragment.ARG_EXERCISE_ID])

    val uiState: StateFlow<ExerciseProgressUiState> = combine(
        flow { emit(getExercise(exerciseId)?.name) },
        savedStateHandle.getStateFlow(KEY_METRIC, ProgressMetric.MAX_WEIGHT),
        observeProgress(exerciseId),
    ) { name, metric, progress ->
        ExerciseProgressUiState(
            isLoading = false,
            exerciseName = name,
            metric = metric,
            points = progress.points,
            records = progress.records,
            executions = progress.executions.map { execution ->
                ExecutionItem(execution, hasRecord = execution.sets.any { it.setId in progress.recordsBySet })
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseProgressUiState())

    fun selectMetric(metric: ProgressMetric) {
        savedStateHandle[KEY_METRIC] = metric
    }

    private companion object {
        const val KEY_METRIC = "metric"
    }
}
