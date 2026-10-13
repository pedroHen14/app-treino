package com.pedro.treinoapp.feature.settings.domain.repository

import com.pedro.treinoapp.feature.settings.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemeSettingsRepository {
    fun observeThemeMode(): Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
