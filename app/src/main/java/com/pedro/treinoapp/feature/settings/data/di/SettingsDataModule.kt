package com.pedro.treinoapp.feature.settings.data.di

import com.pedro.treinoapp.feature.settings.data.ThemeSettingsRepositoryImpl
import com.pedro.treinoapp.feature.settings.domain.repository.ThemeSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsDataModule {

    @Binds
    @Singleton
    abstract fun bindThemeSettingsRepository(impl: ThemeSettingsRepositoryImpl): ThemeSettingsRepository
}
