package com.pedro.treinoapp.feature.settings.domain

import com.pedro.treinoapp.feature.settings.domain.model.ThemeMode
import com.pedro.treinoapp.feature.settings.domain.repository.ThemeSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeThemeSettingsRepository : ThemeSettingsRepository {

    private val mode = MutableStateFlow(ThemeMode.DEFAULT)

    override fun observeThemeMode(): Flow<ThemeMode> = mode

    override suspend fun setThemeMode(mode: ThemeMode) {
        this.mode.value = mode
    }
}
