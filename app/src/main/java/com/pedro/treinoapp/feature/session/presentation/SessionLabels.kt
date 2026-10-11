package com.pedro.treinoapp.feature.session.presentation

import android.content.Context
import com.pedro.treinoapp.R
import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSet
import com.pedro.treinoapp.feature.session.domain.usecase.SetRestTimerDurationUseCase
import java.math.BigDecimal
import kotlin.math.ceil
import kotlin.time.Duration

/** Carga em kg com vírgula e sem zeros à direita: 42500 g → "42,5". */
fun formatWeight(grams: Int): String =
    BigDecimal(grams).movePointLeft(3).stripTrailingZeros().toPlainString().replace('.', ',')

/**
 * Converte o texto digitado (kg, com vírgula ou ponto) em gramas. Texto vazio é nulo.
 * Falha para texto que não é número ou com fração menor que 1 g; o passo de 0,5 kg é
 * validado no domínio.
 */
fun parseWeight(text: String): Result<Int?> {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return Result.success(null)
    val grams = trimmed.replace(',', '.').toBigDecimalOrNull()?.movePointRight(3)
        ?: return Result.failure(NumberFormatException(text))
    return runCatching { grams.intValueExact() }
}

fun parseReps(text: String): Result<Int?> {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return Result.success(null)
    return trimmed.toIntOrNull()?.let { Result.success(it) } ?: Result.failure(NumberFormatException(text))
}

/** "1:30". Arredonda para cima, para o descanso só mostrar 0:00 quando terminar. */
fun Duration.formatClock(): String {
    val seconds = ceil(inWholeMilliseconds / 1000.0).toLong().coerceAtLeast(0)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

fun SessionError.message(context: Context): String = when (this) {
    SessionError.AlreadyActive -> context.getString(R.string.session_error_already_active)
    SessionError.NotFound -> context.getString(R.string.session_error_not_found)
    SessionError.InvalidMakeupDate -> context.getString(R.string.session_error_makeup_date)
    SessionError.InvalidWeight -> context.getString(R.string.session_error_weight)
    SessionError.InvalidReps ->
        context.getString(R.string.session_error_reps, WorkoutSet.MIN_REPS, WorkoutSet.MAX_REPS)
    SessionError.MissingValues -> context.getString(R.string.session_error_missing_values)
    SessionError.TooManySets -> context.getString(R.string.session_error_too_many_sets, WorkoutSet.MAX_SETS)
    SessionError.DuplicateExercise -> context.getString(R.string.session_error_duplicate_exercise)
    SessionError.NoCompletedSets -> context.getString(R.string.session_error_no_completed_sets)
    SessionError.InvalidRestDuration -> context.getString(
        R.string.session_error_rest_duration,
        SetRestTimerDurationUseCase.MIN.formatClock(),
        SetRestTimerDurationUseCase.MAX.formatClock(),
    )
}
