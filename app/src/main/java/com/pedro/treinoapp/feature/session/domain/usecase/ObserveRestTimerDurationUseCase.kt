package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.repository.RestTimerSettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import kotlin.time.Duration

/** Tempo padrão de descanso, usado ao concluir cada série. */
class ObserveRestTimerDurationUseCase @Inject constructor(
    private val settings: RestTimerSettingsRepository,
) {
    operator fun invoke(): Flow<Duration> = settings.observeDefaultDuration()
}
