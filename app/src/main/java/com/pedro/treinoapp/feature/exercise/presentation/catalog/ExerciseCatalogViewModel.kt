package com.pedro.treinoapp.feature.exercise.presentation.catalog

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedro.treinoapp.R
import com.pedro.treinoapp.feature.exercise.domain.model.Exercise
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseError
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseFilter
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseResult
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import com.pedro.treinoapp.feature.exercise.domain.usecase.ArchiveExerciseUseCase
import com.pedro.treinoapp.feature.exercise.domain.usecase.DeleteExerciseUseCase
import com.pedro.treinoapp.feature.exercise.domain.usecase.ObserveExercisesUseCase
import com.pedro.treinoapp.feature.exercise.domain.usecase.UnarchiveExerciseUseCase
import com.pedro.treinoapp.feature.exercise.presentation.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseCatalogUiState(
    val items: List<ExerciseListItem> = emptyList(),
    val isLoading: Boolean = true,
    val selectedCount: Int = 0,
)

sealed interface ExerciseCatalogEvent {
    data class ShowMessage(@StringRes val messageRes: Int) : ExerciseCatalogEvent
    data class Archived(val exercise: Exercise) : ExerciseCatalogEvent

    /** Exclusão bloqueada porque o exercício já foi usado; a tela oferece arquivar. */
    data class OfferArchive(val exercise: Exercise, @StringRes val reasonRes: Int) : ExerciseCatalogEvent
}

/**
 * No modo seleção (aberto pelo editor de modelo) o catálogo esconde arquivados e os exercícios
 * que o modelo já tem, e permite marcar vários. A ordem dos ids selecionados é a ordem dos toques.
 */
@HiltViewModel
class ExerciseCatalogViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observeExercises: ObserveExercisesUseCase,
    private val archiveExercise: ArchiveExerciseUseCase,
    private val unarchiveExercise: UnarchiveExerciseUseCase,
    private val deleteExercise: DeleteExerciseUseCase,
) : ViewModel() {

    val isSelectionMode: Boolean = savedStateHandle[ExerciseCatalogFragment.ARG_SELECTION_MODE] ?: false

    /** Seleção única: o primeiro toque já devolve o exercício (ex.: trocar um exercício da sessão). */
    val isSingleSelection: Boolean = savedStateHandle[ExerciseCatalogFragment.ARG_SINGLE_SELECTION] ?: false

    private val excludedIds: Set<Long> =
        savedStateHandle.get<LongArray>(ExerciseCatalogFragment.ARG_EXCLUDED_IDS)?.toSet().orEmpty()

    private val selectedIds = savedStateHandle.getStateFlow(KEY_SELECTED_IDS, LongArray(0))

    private val _filter = MutableStateFlow(ExerciseFilter())
    val filter: StateFlow<ExerciseFilter> = _filter.asStateFlow()

    val uiState: StateFlow<ExerciseCatalogUiState> = combine(observeExercises(_filter), selectedIds) { exercises, selected ->
        val items = exercises
            .filter { it.id !in excludedIds }
            .map { ExerciseListItem(it, isSelected = it.id in selected) }
        ExerciseCatalogUiState(items = items, isLoading = false, selectedCount = selected.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseCatalogUiState())

    val selection: LongArray get() = selectedIds.value

    fun toggleSelection(exercise: Exercise) {
        val current = selectedIds.value
        savedStateHandle[KEY_SELECTED_IDS] =
            if (exercise.id in current) current.filter { it != exercise.id }.toLongArray() else current + exercise.id
    }

    private val _events = Channel<ExerciseCatalogEvent>(Channel.BUFFERED)
    val events: Flow<ExerciseCatalogEvent> = _events.receiveAsFlow()

    fun onQueryChange(query: String) = _filter.update { it.copy(query = query) }

    fun onMuscleGroupSelected(muscleGroup: MuscleGroup?) = _filter.update { it.copy(muscleGroup = muscleGroup) }

    fun onShowArchivedChange(show: Boolean) = _filter.update { it.copy(includeArchived = show) }

    fun archive(exercise: Exercise) = viewModelScope.launch {
        when (val result = archiveExercise(exercise.id)) {
            is ExerciseResult.Success -> _events.send(ExerciseCatalogEvent.Archived(exercise))
            is ExerciseResult.Failure -> showError(result.error)
        }
    }

    fun unarchive(exercise: Exercise) = viewModelScope.launch {
        when (val result = unarchiveExercise(exercise.id)) {
            is ExerciseResult.Success -> _events.send(ExerciseCatalogEvent.ShowMessage(R.string.exercise_unarchived))
            is ExerciseResult.Failure -> showError(result.error)
        }
    }

    fun delete(exercise: Exercise) = viewModelScope.launch {
        when (val result = deleteExercise(exercise.id)) {
            is ExerciseResult.Success -> _events.send(ExerciseCatalogEvent.ShowMessage(R.string.exercise_deleted))
            is ExerciseResult.Failure -> when (result.error) {
                ExerciseError.UsedInSession, ExerciseError.UsedInTemplate ->
                    _events.send(ExerciseCatalogEvent.OfferArchive(exercise, result.error.messageRes()))
                else -> showError(result.error)
            }
        }
    }

    private suspend fun showError(error: ExerciseError) {
        _events.send(ExerciseCatalogEvent.ShowMessage(error.messageRes()))
    }

    private companion object {
        const val KEY_SELECTED_IDS = "selected_ids"
    }
}
