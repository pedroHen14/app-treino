package com.pedro.treinoapp.feature.exercise.presentation.wger

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedro.treinoapp.R
import com.pedro.treinoapp.feature.exercise.domain.model.Exercise
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseError
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseResult
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import com.pedro.treinoapp.feature.exercise.domain.model.WgerExercise
import com.pedro.treinoapp.feature.exercise.domain.usecase.ImportWgerExerciseUseCase
import com.pedro.treinoapp.feature.exercise.domain.usecase.SearchWgerExercisesUseCase
import com.pedro.treinoapp.feature.exercise.presentation.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface WgerSearchUiState {
    /** Ainda não há termo suficiente para buscar. */
    data object Idle : WgerSearchUiState
    data object Loading : WgerSearchUiState
    data class Results(val items: List<WgerExercise>) : WgerSearchUiState
    data object Empty : WgerSearchUiState

    /** [isNoConnection] distingue a falta de internet de uma falha do wger. */
    data class Error(@StringRes val messageRes: Int, val isNoConnection: Boolean) : WgerSearchUiState
}

/** Formulário de revisão antes de importar: o usuário pode corrigir o nome e escolher o grupo. */
data class WgerImportUiState(
    val exercise: WgerExercise,
    val name: String = exercise.name,
    val muscleGroup: MuscleGroup? = exercise.muscleGroup,
    @StringRes val nameError: Int? = null,
    @StringRes val muscleGroupError: Int? = null,
    @StringRes val generalError: Int? = null,
    val isSaving: Boolean = false,
)

sealed interface WgerSearchEvent {
    /** [importedIds] acumula tudo o que foi importado nesta tela, para devolver ao catálogo. */
    data class Imported(val name: String, val importedIds: List<Long>) : WgerSearchEvent

    /** Importação bloqueada; a tela oferece ver o exercício que já está no catálogo. */
    data class AlreadyInCatalog(val existing: Exercise) : WgerSearchEvent
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class WgerSearchViewModel @Inject constructor(
    private val searchExercises: SearchWgerExercisesUseCase,
    private val importExercise: ImportWgerExerciseUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val retries = MutableStateFlow(0)

    val uiState: StateFlow<WgerSearchUiState> =
        combine(query.debounce(SEARCH_DEBOUNCE_MS).map { it.trim() }.distinctUntilChanged(), retries) { term, _ -> term }
            .flatMapLatest(::search)
            .stateIn(viewModelScope, SharingStarted.Lazily, WgerSearchUiState.Idle)

    private val _importState = MutableStateFlow<WgerImportUiState?>(null)
    val importState: StateFlow<WgerImportUiState?> = _importState.asStateFlow()

    private val _events = Channel<WgerSearchEvent>(Channel.BUFFERED)
    val events: Flow<WgerSearchEvent> = _events.receiveAsFlow()

    private val importedIds = mutableListOf<Long>()

    private fun search(term: String): Flow<WgerSearchUiState> = flow {
        if (term.length < SearchWgerExercisesUseCase.MIN_TERM_LENGTH) {
            emit(WgerSearchUiState.Idle)
            return@flow
        }
        emit(WgerSearchUiState.Loading)
        val state = when (val result = searchExercises(term)) {
            is ExerciseResult.Success ->
                if (result.value.isEmpty()) WgerSearchUiState.Empty else WgerSearchUiState.Results(result.value)
            is ExerciseResult.Failure ->
                WgerSearchUiState.Error(result.error.messageRes(), result.error == ExerciseError.NoConnection)
        }
        emit(state)
    }

    fun onQueryChange(text: String) {
        query.value = text
    }

    fun retry() = retries.update { it + 1 }

    fun onExerciseSelected(exercise: WgerExercise) {
        _importState.value = WgerImportUiState(exercise)
    }

    fun onImportDismissed() {
        _importState.value = null
    }

    fun onNameChange(name: String) = _importState.update { form ->
        if (form == null || form.name == name) form else form.copy(name = name, nameError = null, generalError = null)
    }

    fun onMuscleGroupChange(muscleGroup: MuscleGroup) = _importState.update {
        it?.copy(muscleGroup = muscleGroup, muscleGroupError = null, generalError = null)
    }

    fun import() {
        val form = _importState.value ?: return
        if (form.isSaving) return

        _importState.value = form.copy(isSaving = true)
        viewModelScope.launch {
            when (val result = importExercise(form.exercise, form.name, form.muscleGroup)) {
                is ExerciseResult.Success -> {
                    _importState.value = null
                    importedIds += result.value
                    _events.send(WgerSearchEvent.Imported(form.name.trim(), importedIds.toList()))
                }
                is ExerciseResult.Failure -> {
                    val error = result.error
                    if (error is ExerciseError.AlreadyInCatalog) {
                        _importState.value = null
                        _events.send(WgerSearchEvent.AlreadyInCatalog(error.existing))
                    } else {
                        _importState.update { it?.withError(error)?.copy(isSaving = false) }
                    }
                }
            }
        }
    }

    private fun WgerImportUiState.withError(error: ExerciseError) = when (error) {
        ExerciseError.NameBlank, ExerciseError.NameAlreadyExists -> copy(nameError = error.messageRes())
        ExerciseError.MuscleGroupRequired -> copy(muscleGroupError = error.messageRes())
        else -> copy(generalError = error.messageRes())
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
