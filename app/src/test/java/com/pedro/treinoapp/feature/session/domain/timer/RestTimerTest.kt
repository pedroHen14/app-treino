package com.pedro.treinoapp.feature.session.domain.timer

import com.pedro.treinoapp.feature.session.domain.timer.RestTimerState.Finished
import com.pedro.treinoapp.feature.session.domain.timer.RestTimerState.Idle
import com.pedro.treinoapp.feature.session.domain.timer.RestTimerState.Paused
import com.pedro.treinoapp.feature.session.domain.timer.RestTimerState.Running
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerTest {

    private fun TestScope.timer() = RestTimer(backgroundScope, testScheduler.timeSource)

    private fun TestScope.advance(seconds: Int) {
        advanceTimeBy(seconds.seconds)
        runCurrent()
    }

    @Test
    fun `counts down and finishes`() = runTest {
        val timer = timer()

        timer.start(90.seconds)
        runCurrent()
        assertEquals(Running(90.seconds, 90.seconds), timer.state.value)

        advance(30)
        assertEquals(Running(60.seconds, 90.seconds), timer.state.value)

        advance(60)
        assertEquals(Finished, timer.state.value)
    }

    @Test
    fun `pause keeps the remaining time until resumed`() = runTest {
        val timer = timer()
        timer.start(60.seconds)
        advance(20)

        timer.pause()
        advance(100)
        assertEquals(Paused(40.seconds, 60.seconds), timer.state.value)

        timer.resume()
        advance(10)
        assertEquals(Running(30.seconds, 60.seconds), timer.state.value)
    }

    @Test
    fun `adds and subtracts 15 seconds while running`() = runTest {
        val timer = timer()
        timer.start(60.seconds)
        advance(10)

        timer.adjust(RestTimer.ADJUST_STEP)
        runCurrent()
        assertEquals(Running(65.seconds, 75.seconds), timer.state.value)

        timer.adjust(-RestTimer.ADJUST_STEP)
        timer.adjust(-RestTimer.ADJUST_STEP)
        runCurrent()
        assertEquals(Running(35.seconds, 45.seconds), timer.state.value)
    }

    @Test
    fun `subtracting past zero finishes the rest`() = runTest {
        val timer = timer()
        timer.start(20.seconds)
        advance(10)

        timer.adjust(-RestTimer.ADJUST_STEP)
        runCurrent()

        assertEquals(Finished, timer.state.value)
    }

    @Test
    fun `adjusts while paused`() = runTest {
        val timer = timer()
        timer.start(30.seconds)
        advance(10)
        timer.pause()

        timer.adjust(RestTimer.ADJUST_STEP)
        assertEquals(Paused(35.seconds, 45.seconds), timer.state.value)

        timer.adjust(-RestTimer.ADJUST_STEP)
        timer.adjust(-RestTimer.ADJUST_STEP)
        timer.adjust(-RestTimer.ADJUST_STEP)
        assertEquals(Finished, timer.state.value)
    }

    @Test
    fun `skip stops without finishing`() = runTest {
        val timer = timer()
        timer.start(30.seconds)
        advance(5)

        timer.skip()
        advance(60)

        assertEquals(Idle, timer.state.value)
    }

    @Test
    fun `starting again restarts with the new duration`() = runTest {
        val timer = timer()
        timer.start(30.seconds)
        advance(25)

        timer.start(90.seconds)
        advance(10)

        assertEquals(Running(80.seconds, 90.seconds), timer.state.value)
    }
}
