package com.pedro.treinoapp.feature.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.pedro.treinoapp.feature.settings.domain.model.ThemeMode
import com.pedro.treinoapp.feature.settings.domain.repository.ThemeSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ThemeSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ThemeSettingsRepository {

    override fun observeThemeMode(): Flow<ThemeMode> =
        dataStore.data
            .map { ThemeMode.fromStorage(it[KEY_THEME_MODE]) }
            .distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
