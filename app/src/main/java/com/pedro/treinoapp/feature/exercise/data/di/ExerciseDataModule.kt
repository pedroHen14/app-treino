package com.pedro.treinoapp.feature.exercise.data.di

import androidx.room.RoomDatabase
import com.pedro.treinoapp.core.database.AppDatabase
import com.pedro.treinoapp.feature.exercise.data.ExerciseRepositoryImpl
import com.pedro.treinoapp.feature.exercise.data.local.ExerciseDao
import com.pedro.treinoapp.feature.exercise.data.seed.ExerciseCatalogSeedCallback
import com.pedro.treinoapp.feature.exercise.domain.repository.ExerciseRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExerciseDataModule {

    @Binds
    @Singleton
    abstract fun bindExerciseRepository(impl: ExerciseRepositoryImpl): ExerciseRepository

    @Binds
    @IntoSet
    abstract fun bindSeedCallback(callback: ExerciseCatalogSeedCallback): RoomDatabase.Callback

    companion object {
        @Provides
        fun provideExerciseDao(database: AppDatabase): ExerciseDao = database.exerciseDao()
    }
}
