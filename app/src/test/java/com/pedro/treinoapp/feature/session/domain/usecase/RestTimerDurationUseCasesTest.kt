package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.FakeRestTimerSettingsRepository
import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class RestTimerDurationUseCasesTest {

    private val settings = FakeRestTimerSettingsRepository()
    private val observe = ObserveRestTimerDurationUseCase(settings)
    private val set = SetRestTimerDurationUseCase(settings)

    @Test
    fun `default rest is 90 seconds`() = runTest {
        assertEquals(90.seconds, observe().first())
    }

    @Test
    fun `default rest can be changed`() = runTest {
        assertEquals(SessionResult.Success(Unit), set(2.minutes))

        assertEquals(2.minutes, observe().first())
    }

    @Test
    fun `rest must be between 15 seconds and 10 minutes in whole seconds`() = runTest {
        val invalid = SessionResult.Failure(SessionError.InvalidRestDuration)

        assertEquals(invalid, set(10.seconds))
        assertEquals(invalid, set(11.minutes))
        assertEquals(invalid, set(30.seconds + 500.milliseconds))
        assertEquals(SessionResult.Success(Unit), set(15.seconds))
        assertEquals(SessionResult.Success(Unit), set(10.minutes))
    }
}
