package com.pedro.treinoapp.feature.session.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.pedro.treinoapp.feature.session.domain.repository.RestTimerSettingsRepository
import com.pedro.treinoapp.feature.session.domain.usecase.SetRestTimerDurationUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class RestTimerSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : RestTimerSettingsRepository {

    override fun observeDefaultDuration(): Flow<Duration> =
        dataStore.data
            .map { (it[KEY_DEFAULT_SECONDS] ?: SetRestTimerDurationUseCase.DEFAULT.inWholeSeconds.toInt()).seconds }
            .distinctUntilChanged()

    override suspend fun setDefaultDuration(duration: Duration) {
        dataStore.edit { it[KEY_DEFAULT_SECONDS] = duration.inWholeSeconds.toInt() }
    }

    private companion object {
        val KEY_DEFAULT_SECONDS = intPreferencesKey("rest_timer_default_seconds")
    }
}
