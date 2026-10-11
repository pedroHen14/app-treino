package com.pedro.treinoapp.feature.workout.presentation.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedro.treinoapp.feature.exercise.domain.usecase.GetExerciseUseCase
import com.pedro.treinoapp.feature.workout.domain.model.RepRange
import com.pedro.treinoapp.feature.workout.domain.model.TemplateExercise
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutError
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutResult
import com.pedro.treinoapp.feature.workout.domain.usecase.CreateWorkoutTemplateUseCase
import com.pedro.treinoapp.feature.workout.domain.usecase.GetWorkoutTemplateUseCase
import com.pedro.treinoapp.feature.workout.domain.usecase.UpdateWorkoutTemplateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Linha do editor. Séries e repetições ficam como texto enquanto o usuário digita;
 * a conversão e a validação acontecem ao salvar. Reps máx. vazio significa valor fixo.
 */
data class EditorExercise(
    val exerciseId: Long,
    val exerciseName: String,
    val sets: String,
    val repsMin: String,
    val repsMax: String,
    val error: WorkoutError? = null,
)

data class TemplateEditorUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val name: String = "",
    val exercises: List<EditorExercise> = emptyList(),
    val nameError: WorkoutError? = null,
    val listError: WorkoutError? = null,
    val generalError: WorkoutError? = null,
    val hasChanges: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

@HiltViewModel
class TemplateEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTemplate: GetWorkoutTemplateUseCase,
    private val getExercise: GetExerciseUseCase,
    private val createTemplate: CreateWorkoutTemplateUseCase,
    private val updateTemplate: UpdateWorkoutTemplateUseCase,
) : ViewModel() {

    private val templateId: Long? = savedStateHandle.get<Long>(TemplateEditorFragment.ARG_TEMPLATE_ID)
        ?.takeIf { it != TemplateEditorFragment.NEW_TEMPLATE }

    private val _uiState = MutableStateFlow(TemplateEditorUiState(isEditing = templateId != null))
    val uiState: StateFlow<TemplateEditorUiState> = _uiState.asStateFlow()

    /** O que estava salvo ao abrir, para saber se há alterações a descartar. */
    private var original: Pair<String, List<EditorExercise>> = "" to emptyList()

    init {
        templateId?.let(::load)
    }

    val exerciseIds: LongArray get() = _uiState.value.exercises.map { it.exerciseId }.toLongArray()

    private fun load(id: Long) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        val template = getTemplate(id)
        if (template == null) {
            _uiState.update { it.copy(isLoading = false, generalError = WorkoutError.NotFound) }
            return@launch
        }
        val exercises = template.exercises.map { it.toEditor() }
        original = template.name to exercises
        _uiState.update { it.copy(isLoading = false, name = template.name, exercises = exercises) }
    }

    fun onNameChange(name: String) {
        if (name == _uiState.value.name) return
        edit { it.copy(name = name, nameError = null) }
    }

    /** Ids já presentes são ignorados: o mesmo exercício não se repete no modelo. */
    fun addExercises(ids: LongArray) = viewModelScope.launch {
        val present = _uiState.value.exercises.map { it.exerciseId }.toSet()
        val added = ids.filter { it !in present }.mapNotNull { id ->
            getExercise(id)?.let { exercise ->
                val defaults = TemplateExercise.DEFAULT_REPS
                EditorExercise(
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    sets = TemplateExercise.DEFAULT_SETS.toString(),
                    repsMin = defaults.min.toString(),
                    repsMax = defaults.max.toString(),
                )
            }
        }
        if (added.isEmpty()) return@launch
        edit { it.copy(exercises = it.exercises + added, listError = null) }
    }

    fun removeExercise(exerciseId: Long) =
        edit { state -> state.copy(exercises = state.exercises.filterNot { it.exerciseId == exerciseId }) }

    fun moveExercise(from: Int, to: Int) = edit { state ->
        val list = state.exercises.toMutableList()
        if (from !in list.indices || to !in list.indices) return@edit state
        list.add(to, list.removeAt(from))
        state.copy(exercises = list)
    }

    fun onSetsChange(exerciseId: Long, value: String) = editExercise(exerciseId) { it.copy(sets = value) }

    fun onRepsMinChange(exerciseId: Long, value: String) = editExercise(exerciseId) { it.copy(repsMin = value) }

    fun onRepsMaxChange(exerciseId: Long, value: String) = editExercise(exerciseId) { it.copy(repsMax = value) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return

        val exercises = state.exercises.map { it.toDomain() }
        _uiState.update { it.copy(isSaving = true, generalError = null) }
        viewModelScope.launch {
            val result = if (templateId == null) {
                createTemplate(state.name, exercises)
            } else {
                updateTemplate(templateId, state.name, exercises)
            }
            _uiState.update {
                when (result) {
                    is WorkoutResult.Success -> it.copy(isSaving = false, isSaved = true)
                    is WorkoutResult.Failure -> it.withError(result.error).copy(isSaving = false)
                }
            }
        }
    }

    private fun editExercise(exerciseId: Long, change: (EditorExercise) -> EditorExercise) = edit { state ->
        state.copy(
            exercises = state.exercises.map {
                if (it.exerciseId == exerciseId) change(it).copy(error = null) else it
            },
        )
    }

    private fun edit(change: (TemplateEditorUiState) -> TemplateEditorUiState) = _uiState.update { state ->
        val changed = change(state)
        val current = changed.name to changed.exercises.map { it.copy(error = null) }
        changed.copy(hasChanges = current != original)
    }

    private fun TemplateEditorUiState.withError(error: WorkoutError) = when (error) {
        WorkoutError.NameBlank, WorkoutError.NameAlreadyExists -> copy(nameError = error)
        WorkoutError.NoExercises -> copy(listError = error)
        is WorkoutError.DuplicateExercise -> copy(exercises = exercises.withErrorAt(error.index, error))
        is WorkoutError.InvalidSets -> copy(exercises = exercises.withErrorAt(error.index, error))
        is WorkoutError.InvalidReps -> copy(exercises = exercises.withErrorAt(error.index, error))
        WorkoutError.NotFound -> copy(generalError = error)
    }

    private fun List<EditorExercise>.withErrorAt(index: Int, error: WorkoutError) =
        mapIndexed { i, it -> if (i == index) it.copy(error = error) else it }

    private fun TemplateExercise.toEditor() = EditorExercise(
        exerciseId = exerciseId,
        exerciseName = exerciseName,
        sets = targetSets.toString(),
        repsMin = targetReps.min.toString(),
        repsMax = if (targetReps.isFixed) "" else targetReps.max.toString(),
    )

    /** Texto inválido vira 0, que a validação do domínio rejeita com a mensagem certa. */
    private fun EditorExercise.toDomain(): TemplateExercise {
        val min = repsMin.trim().toIntOrNull() ?: 0
        val max = repsMax.trim().takeIf { it.isNotEmpty() }?.let { it.toIntOrNull() ?: 0 } ?: min
        return TemplateExercise(
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            targetSets = sets.trim().toIntOrNull() ?: 0,
            targetReps = RepRange(min, max),
        )
    }
}
