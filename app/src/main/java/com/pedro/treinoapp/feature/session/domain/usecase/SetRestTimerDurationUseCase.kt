package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.repository.RestTimerSettingsRepository
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class SetRestTimerDurationUseCase @Inject constructor(
    private val settings: RestTimerSettingsRepository,
) {
    suspend operator fun invoke(duration: Duration): SessionResult<Unit> {
        if (duration !in RANGE || duration.inWholeMilliseconds % 1000 != 0L) {
            return SessionResult.Failure(SessionError.InvalidRestDuration)
        }
        settings.setDefaultDuration(duration)
        return SessionResult.Success(Unit)
    }

    companion object {
        val DEFAULT: Duration = 90.seconds
        val MIN: Duration = 15.seconds
        val MAX: Duration = 10.minutes
        val RANGE: ClosedRange<Duration> = MIN..MAX
    }
}
