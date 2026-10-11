package com.pedro.treinoapp.feature.session.domain.model

sealed interface SessionResult<out T> {
    data class Success<T>(val value: T) : SessionResult<T>
    data class Failure(val error: SessionError) : SessionResult<Nothing>
}

sealed interface SessionError {
    /** Já existe uma sessão em andamento; só pode haver uma por vez. */
    data object AlreadyActive : SessionError

    /** Sessão, modelo, exercício ou série inexistente, ou sessão já finalizada. */
    data object NotFound : SessionError
    data object InvalidMakeupDate : SessionError
    data object InvalidWeight : SessionError
    data object InvalidReps : SessionError

    /** Série concluída precisa de carga e repetições. */
    data object MissingValues : SessionError
    data object TooManySets : SessionError
    data object DuplicateExercise : SessionError
    data object NoCompletedSets : SessionError
    data object InvalidRestDuration : SessionError
}
