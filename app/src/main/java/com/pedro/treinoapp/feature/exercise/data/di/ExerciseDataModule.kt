package com.pedro.treinoapp.feature.exercise.data.di

import androidx.room.RoomDatabase
import com.pedro.treinoapp.core.database.AppDatabase
import com.pedro.treinoapp.feature.exercise.data.ExerciseRepositoryImpl
import com.pedro.treinoapp.feature.exercise.data.WgerExerciseRepositoryImpl
import com.pedro.treinoapp.feature.exercise.data.local.ExerciseDao
import com.pedro.treinoapp.feature.exercise.data.remote.WgerApi
import com.pedro.treinoapp.feature.exercise.data.seed.ExerciseCatalogSeedCallback
import com.pedro.treinoapp.feature.exercise.domain.repository.ExerciseRepository
import com.pedro.treinoapp.feature.exercise.domain.repository.WgerExerciseRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
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

    @Binds
    @Singleton
    abstract fun bindWgerExerciseRepository(impl: WgerExerciseRepositoryImpl): WgerExerciseRepository

    companion object {
        @Provides
        fun provideExerciseDao(database: AppDatabase): ExerciseDao = database.exerciseDao()

        @Provides
        @Singleton
        fun provideWgerApi(client: OkHttpClient, json: Json): WgerApi = Retrofit.Builder()
            .baseUrl(WgerApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WgerApi::class.java)
    }
}
