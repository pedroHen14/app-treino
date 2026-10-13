package com.pedro.treinoapp.feature.settings.domain.usecase

import com.pedro.treinoapp.feature.settings.domain.model.ThemeMode
import com.pedro.treinoapp.feature.settings.domain.repository.ThemeSettingsRepository
import javax.inject.Inject

class SetThemeModeUseCase @Inject constructor(
    private val settings: ThemeSettingsRepository,
) {
    suspend operator fun invoke(mode: ThemeMode) = settings.setThemeMode(mode)
}
