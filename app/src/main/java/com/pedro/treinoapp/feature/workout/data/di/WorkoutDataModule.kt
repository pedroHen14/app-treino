package com.pedro.treinoapp.feature.workout.data.di

import com.pedro.treinoapp.core.database.AppDatabase
import com.pedro.treinoapp.feature.workout.data.WeeklyScheduleRepositoryImpl
import com.pedro.treinoapp.feature.workout.data.WorkoutTemplateRepositoryImpl
import com.pedro.treinoapp.feature.workout.data.local.WeeklyScheduleDao
import com.pedro.treinoapp.feature.workout.data.local.WorkoutTemplateDao
import com.pedro.treinoapp.feature.workout.domain.repository.WeeklyScheduleRepository
import com.pedro.treinoapp.feature.workout.domain.repository.WorkoutTemplateRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkoutDataModule {

    @Binds
    @Singleton
    abstract fun bindWorkoutTemplateRepository(impl: WorkoutTemplateRepositoryImpl): WorkoutTemplateRepository

    @Binds
    @Singleton
    abstract fun bindWeeklyScheduleRepository(impl: WeeklyScheduleRepositoryImpl): WeeklyScheduleRepository

    companion object {
        @Provides
        fun provideWorkoutTemplateDao(database: AppDatabase): WorkoutTemplateDao = database.workoutTemplateDao()

        @Provides
        fun provideWeeklyScheduleDao(database: AppDatabase): WeeklyScheduleDao = database.weeklyScheduleDao()
    }
}
