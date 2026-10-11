package com.pedro.treinoapp.feature.workout.domain.model

sealed interface WorkoutResult<out T> {
    data class Success<T>(val value: T) : WorkoutResult<T>
    data class Failure(val error: WorkoutError) : WorkoutResult<Nothing>
}

sealed interface WorkoutError {
    data object NameBlank : WorkoutError
    data object NameAlreadyExists : WorkoutError
    data object NoExercises : WorkoutError
    data object NotFound : WorkoutError

    /** `index` é a posição do exercício com problema na lista do modelo. */
    data class DuplicateExercise(val index: Int) : WorkoutError
    data class InvalidSets(val index: Int) : WorkoutError
    data class InvalidReps(val index: Int) : WorkoutError
}
