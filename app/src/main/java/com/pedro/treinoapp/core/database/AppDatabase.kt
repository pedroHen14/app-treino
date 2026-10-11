package com.pedro.treinoapp.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pedro.treinoapp.feature.exercise.data.local.ExerciseDao
import com.pedro.treinoapp.feature.exercise.data.local.ExerciseEntity
import com.pedro.treinoapp.feature.session.data.local.SessionDao
import com.pedro.treinoapp.feature.session.data.local.SessionExerciseEntity
import com.pedro.treinoapp.feature.session.data.local.WorkoutSessionEntity
import com.pedro.treinoapp.feature.session.data.local.WorkoutSetEntity
import com.pedro.treinoapp.feature.workout.data.local.TemplateExerciseEntity
import com.pedro.treinoapp.feature.workout.data.local.WeeklyScheduleDao
import com.pedro.treinoapp.feature.workout.data.local.WeeklyScheduleEntity
import com.pedro.treinoapp.feature.workout.data.local.WorkoutTemplateDao
import com.pedro.treinoapp.feature.workout.data.local.WorkoutTemplateEntity

/**
 * Versão 2: carga e repetições da série passam a aceitar nulo (série não preenchida) e
 * `session_exercise` guarda o alvo de repetições do modelo.
 */
@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutTemplateEntity::class,
        TemplateExerciseEntity::class,
        WeeklyScheduleEntity::class,
        WorkoutSessionEntity::class,
        SessionExerciseEntity::class,
        WorkoutSetEntity::class,
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao

    abstract fun workoutTemplateDao(): WorkoutTemplateDao

    abstract fun weeklyScheduleDao(): WeeklyScheduleDao

    abstract fun sessionDao(): SessionDao

    companion object {
        const val NAME = "treino.db"
    }
}
