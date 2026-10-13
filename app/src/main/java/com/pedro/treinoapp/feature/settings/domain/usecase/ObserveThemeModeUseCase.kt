package com.pedro.treinoapp.feature.settings.domain.usecase

import com.pedro.treinoapp.feature.settings.domain.model.ThemeMode
import com.pedro.treinoapp.feature.settings.domain.repository.ThemeSettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveThemeModeUseCase @Inject constructor(
    private val settings: ThemeSettingsRepository,
) {
    operator fun invoke(): Flow<ThemeMode> = settings.observeThemeMode()
}
