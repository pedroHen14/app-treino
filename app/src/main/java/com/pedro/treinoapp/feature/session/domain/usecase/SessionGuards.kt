package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSession
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSet
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository

/** Séries e exercícios podem ser alterados tanto na sessão em andamento quanto numa já finalizada (edição do histórico). */
internal suspend fun SessionRepository.getEditable(sessionId: Long): WorkoutSession? = get(sessionId)

/** Concluir séries, finalizar e descartar só valem para a sessão em andamento. */
internal suspend fun SessionRepository.getActive(sessionId: Long): WorkoutSession? =
    get(sessionId)?.takeIf { it.isActive }

/** Carga em múltiplos de 0,5 kg e repetições a partir de 1. Valores nulos são série não preenchida. */
internal fun validateSetValues(weightGrams: Int?, reps: Int?): SessionError? = when {
    weightGrams != null &&
        (weightGrams !in 0..WorkoutSet.MAX_WEIGHT_GRAMS || weightGrams % WorkoutSet.WEIGHT_STEP_GRAMS != 0) ->
        SessionError.InvalidWeight
    reps != null && reps !in WorkoutSet.MIN_REPS..WorkoutSet.MAX_REPS -> SessionError.InvalidReps
    else -> null
}
