package com.pedro.treinoapp.feature.history.data.di

import com.pedro.treinoapp.core.database.AppDatabase
import com.pedro.treinoapp.feature.history.data.HistoryRepositoryImpl
import com.pedro.treinoapp.feature.history.data.local.HistoryDao
import com.pedro.treinoapp.feature.history.domain.repository.HistoryRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HistoryDataModule {

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    companion object {
        @Provides
        fun provideHistoryDao(database: AppDatabase): HistoryDao = database.historyDao()
    }
}
