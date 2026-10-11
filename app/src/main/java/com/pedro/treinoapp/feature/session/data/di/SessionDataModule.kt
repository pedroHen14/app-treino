package com.pedro.treinoapp.feature.session.data.di

import com.pedro.treinoapp.core.database.AppDatabase
import com.pedro.treinoapp.feature.session.data.SessionRepositoryImpl
import com.pedro.treinoapp.feature.session.data.local.SessionDao
import com.pedro.treinoapp.feature.session.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionDataModule {

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository

    companion object {
        @Provides
        fun provideSessionDao(database: AppDatabase): SessionDao = database.sessionDao()
    }
}
