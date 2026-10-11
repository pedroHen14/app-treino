package com.pedro.treinoapp.feature.workout.presentation

import android.content.Context
import androidx.annotation.StringRes
import com.pedro.treinoapp.R
import com.pedro.treinoapp.feature.workout.domain.model.RepRange
import com.pedro.treinoapp.feature.workout.domain.model.TemplateExercise
import com.pedro.treinoapp.feature.workout.domain.model.WorkoutError
import java.time.DayOfWeek

@StringRes
fun DayOfWeek.labelRes(): Int = when (this) {
    DayOfWeek.MONDAY -> R.string.day_monday
    DayOfWeek.TUESDAY -> R.string.day_tuesday
    DayOfWeek.WEDNESDAY -> R.string.day_wednesday
    DayOfWeek.THURSDAY -> R.string.day_thursday
    DayOfWeek.FRIDAY -> R.string.day_friday
    DayOfWeek.SATURDAY -> R.string.day_saturday
    DayOfWeek.SUNDAY -> R.string.day_sunday
}

@StringRes
fun DayOfWeek.shortLabelRes(): Int = when (this) {
    DayOfWeek.MONDAY -> R.string.day_monday_short
    DayOfWeek.TUESDAY -> R.string.day_tuesday_short
    DayOfWeek.WEDNESDAY -> R.string.day_wednesday_short
    DayOfWeek.THURSDAY -> R.string.day_thursday_short
    DayOfWeek.FRIDAY -> R.string.day_friday_short
    DayOfWeek.SATURDAY -> R.string.day_saturday_short
    DayOfWeek.SUNDAY -> R.string.day_sunday_short
}

/** "segunda e quinta", "segunda, terça e sexta": para usar no meio de uma frase. */
fun List<DayOfWeek>.joinLabels(context: Context): String {
    val locale = context.resources.configuration.locales[0]
    val labels = map { context.getString(it.labelRes()).lowercase(locale) }
    return when (labels.size) {
        0 -> ""
        1 -> labels.single()
        else -> context.getString(R.string.list_last_separator, labels.dropLast(1).joinToString(", "), labels.last())
    }
}

/** "10" ou "8–12". */
fun RepRange.format(): String = if (isFixed) "$min" else "$min–$max"

fun WorkoutError.message(context: Context): String = when (this) {
    WorkoutError.NameBlank -> context.getString(R.string.template_error_name_blank)
    WorkoutError.NameAlreadyExists -> context.getString(R.string.template_error_name_exists)
    WorkoutError.NoExercises -> context.getString(R.string.template_error_no_exercises)
    WorkoutError.NotFound -> context.getString(R.string.template_error_not_found)
    is WorkoutError.DuplicateExercise -> context.getString(R.string.template_error_duplicate_exercise)
    is WorkoutError.InvalidSets ->
        context.getString(R.string.template_error_sets, TemplateExercise.MIN_SETS, TemplateExercise.MAX_SETS)
    is WorkoutError.InvalidReps ->
        context.getString(R.string.template_error_reps, RepRange.MIN_REPS, RepRange.MAX_REPS)
}
