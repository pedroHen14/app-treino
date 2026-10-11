package com.pedro.treinoapp.feature.session.domain

import com.pedro.treinoapp.feature.session.domain.repository.RestTimerSettingsRepository
import com.pedro.treinoapp.feature.session.domain.usecase.SetRestTimerDurationUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Duration

class FakeRestTimerSettingsRepository : RestTimerSettingsRepository {

    private val duration = MutableStateFlow(SetRestTimerDurationUseCase.DEFAULT)

    override fun observeDefaultDuration(): Flow<Duration> = duration

    override suspend fun setDefaultDuration(duration: Duration) {
        this.duration.value = duration
    }
}
