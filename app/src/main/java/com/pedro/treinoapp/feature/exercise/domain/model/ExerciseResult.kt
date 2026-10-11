package com.pedro.treinoapp.feature.exercise.domain.model

sealed interface ExerciseResult<out T> {
    data class Success<T>(val value: T) : ExerciseResult<T>
    data class Failure(val error: ExerciseError) : ExerciseResult<Nothing>
}

sealed interface ExerciseError {
    data object NameBlank : ExerciseError
    data object NameAlreadyExists : ExerciseError
    data object NotFound : ExerciseError

    /** Exercícios do catálogo inicial só podem ser arquivados. */
    data object NotCustom : ExerciseError
    data object UsedInSession : ExerciseError
    data object UsedInTemplate : ExerciseError
}
