package com.pedro.treinoapp.feature.exercise.presentation

import androidx.annotation.StringRes
import com.pedro.treinoapp.R
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseError
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup

@StringRes
fun MuscleGroup.labelRes(): Int = when (this) {
    MuscleGroup.CHEST -> R.string.muscle_group_chest
    MuscleGroup.BACK -> R.string.muscle_group_back
    MuscleGroup.SHOULDERS -> R.string.muscle_group_shoulders
    MuscleGroup.BICEPS -> R.string.muscle_group_biceps
    MuscleGroup.TRICEPS -> R.string.muscle_group_triceps
    MuscleGroup.FOREARMS -> R.string.muscle_group_forearms
    MuscleGroup.QUADRICEPS -> R.string.muscle_group_quadriceps
    MuscleGroup.HAMSTRINGS -> R.string.muscle_group_hamstrings
    MuscleGroup.GLUTES -> R.string.muscle_group_glutes
    MuscleGroup.CALVES -> R.string.muscle_group_calves
    MuscleGroup.ABS -> R.string.muscle_group_abs
}

@StringRes
fun ExerciseError.messageRes(): Int = when (this) {
    ExerciseError.NameBlank -> R.string.exercise_error_name_blank
    ExerciseError.NameAlreadyExists -> R.string.exercise_error_name_exists
    ExerciseError.NotFound -> R.string.exercise_error_not_found
    ExerciseError.NotCustom -> R.string.exercise_error_not_custom
    ExerciseError.UsedInSession -> R.string.exercise_error_used_in_session
    ExerciseError.UsedInTemplate -> R.string.exercise_error_used_in_template
}
