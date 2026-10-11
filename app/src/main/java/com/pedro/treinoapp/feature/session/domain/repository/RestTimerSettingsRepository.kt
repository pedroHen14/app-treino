package com.pedro.treinoapp.feature.session.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface RestTimerSettingsRepository {
    fun observeDefaultDuration(): Flow<Duration>
    suspend fun setDefaultDuration(duration: Duration)
}
