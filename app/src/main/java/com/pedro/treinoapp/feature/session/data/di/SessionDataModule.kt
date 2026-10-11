package com.pedro.treinoapp.feature.session.data.di

import com.pedro.treinoapp.core.database.AppDatabase
import com.pedro.treinoapp.core.di.ApplicationScope
import com.pedro.treinoapp.feature.session.data.SessionRepositoryImpl
import com.pedro.treinoapp.feature.session.data.local.SessionDao
import com.pedro.treinoapp.feature.session.data.settings.RestTimerSettingsRepositoryImpl
import com.pedro.treinoapp.feature.session.domain.repository.RestTimerSettingsRepository
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import com.pedro.treinoapp.feature.session.domain.timer.RestTimer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton
import kotlin.time.TimeSource

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionDataModule {

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    @Binds
    @Singleton
    abstract fun bindRestTimerSettingsRepository(impl: RestTimerSettingsRepositoryImpl): RestTimerSettingsRepository

    companion object {
        @Provides
        fun provideSessionDao(database: AppDatabase): SessionDao = database.sessionDao()

        /** Um timer só para o app inteiro: o descanso continua ao trocar de tela. */
        @Provides
        @Singleton
        fun provideRestTimer(
            @ApplicationScope scope: CoroutineScope,
            timeSource: TimeSource.WithComparableMarks,
        ): RestTimer = RestTimer(scope, timeSource)
    }
}
