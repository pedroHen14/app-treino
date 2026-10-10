package com.pedro.treinoapp.feature.exercise.presentation.form

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pedro.treinoapp.R
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseError
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseResult
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import com.pedro.treinoapp.feature.exercise.domain.usecase.CreateCustomExerciseUseCase
import com.pedro.treinoapp.feature.exercise.domain.usecase.GetExerciseUseCase
import com.pedro.treinoapp.feature.exercise.domain.usecase.UpdateCustomExerciseUseCase
import com.pedro.treinoapp.feature.exercise.presentation.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseFormUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val name: String = "",
    val muscleGroup: MuscleGroup? = null,
    @StringRes val nameError: Int? = null,
    @StringRes val muscleGroupError: Int? = null,
    @StringRes val generalError: Int? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

@HiltViewModel
class ExerciseFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getExercise: GetExerciseUseCase,
    private val createExercise: CreateCustomExerciseUseCase,
    private val updateExercise: UpdateCustomExerciseUseCase,
) : ViewModel() {

    private val exerciseId: Long? = savedStateHandle.get<Long>(ExerciseFormBottomSheet.ARG_EXERCISE_ID)
        ?.takeIf { it != ExerciseFormBottomSheet.NEW_EXERCISE }

    private val _uiState = MutableStateFlow(ExerciseFormUiState(isEditing = exerciseId != null))
    val uiState: StateFlow<ExerciseFormUiState> = _uiState.asStateFlow()

    init {
        exerciseId?.let(::load)
    }

    private fun load(id: Long) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        val exercise = getExercise(id)
        _uiState.update {
            if (exercise == null) {
                it.copy(isLoading = false, generalError = R.string.exercise_error_not_found)
            } else {
                it.copy(isLoading = false, name = exercise.name, muscleGroup = exercise.muscleGroup)
            }
        }
    }

    fun onNameChange(name: String) {
        if (name == _uiState.value.name) return
        _uiState.update { it.copy(name = name, nameError = null, generalError = null) }
    }

    fun onMuscleGroupChange(muscleGroup: MuscleGroup) =
        _uiState.update { it.copy(muscleGroup = muscleGroup, muscleGroupError = null, generalError = null) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving || state.isLoading) return
        val muscleGroup = state.muscleGroup
        if (muscleGroup == null) {
            _uiState.update { it.copy(muscleGroupError = R.string.exercise_error_muscle_group_required) }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = if (exerciseId == null) {
                createExercise(state.name, muscleGroup)
            } else {
                updateExercise(exerciseId, state.name, muscleGroup)
            }
            _uiState.update {
                when (result) {
                    is ExerciseResult.Success -> it.copy(isSaving = false, isSaved = true)
                    is ExerciseResult.Failure -> it.withError(result.error).copy(isSaving = false)
                }
            }
        }
    }

    private fun ExerciseFormUiState.withError(error: ExerciseError) = when (error) {
        ExerciseError.NameBlank, ExerciseError.NameAlreadyExists -> copy(nameError = error.messageRes())
        else -> copy(generalError = error.messageRes())
    }
}
