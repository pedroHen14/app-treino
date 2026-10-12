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

    /** Importação bloqueada: o catálogo já tem esse exercício, que é sugerido no lugar. */
    data class AlreadyInCatalog(val existing: Exercise) : ExerciseError
    data object MuscleGroupRequired : ExerciseError

    /** Sem internet ou o servidor não respondeu a tempo. */
    data object NoConnection : ExerciseError

    /** O wger respondeu com erro ou num formato inesperado. */
    data object ServiceUnavailable : ExerciseError
}
