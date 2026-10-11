package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.FakeSessionRepository
import com.pedro.treinoapp.feature.session.domain.NOW
import com.pedro.treinoapp.feature.session.domain.completedSet
import com.pedro.treinoapp.feature.session.domain.fixedClock
import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSet
import com.pedro.treinoapp.feature.session.domain.session
import com.pedro.treinoapp.feature.session.domain.sessionExercise
import com.pedro.treinoapp.feature.session.domain.timer.RestTimer
import com.pedro.treinoapp.feature.session.domain.timer.RestTimerState
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

class FinishWorkoutSessionUseCaseTest {

    private fun TestScope.timer() = RestTimer(backgroundScope, testScheduler.timeSource)

    @Test
    fun `finishing saves only completed sets and drops exercises left empty`() = runTest {
        val repository = FakeSessionRepository(
            listOf(
                session(
                    10,
                    finished = false,
                    exercises = arrayOf(
                        sessionExercise(100, 1, completedSet(1, 60_000, 10), WorkoutSet(id = 2, weightGrams = 60_000, reps = 10)),
                        sessionExercise(200, 2, WorkoutSet(id = 3)),
                    ),
                ),
            ),
        )
        assertEquals(2, repository.current.first().pendingSetCount)

        val result = FinishWorkoutSessionUseCase(repository, timer(), fixedClock)(sessionId = 10)

        assertEquals(SessionResult.Success(Unit), result)
        val saved = repository.current.first()
        assertEquals(NOW, saved.finishedAt)
        assertEquals(listOf(1L), saved.exercises.map { it.exerciseId })
        assertEquals(listOf(1L), saved.exercises.first().sets.map { it.id })
    }

    @Test
    fun `session without completed sets cannot be saved`() = runTest {
        val repository = FakeSessionRepository(
            listOf(session(10, finished = false, exercises = arrayOf(sessionExercise(100, 1, WorkoutSet(id = 1))))),
        )

        val result = FinishWorkoutSessionUseCase(repository, timer(), fixedClock)(sessionId = 10)

        assertEquals(SessionResult.Failure(SessionError.NoCompletedSets), result)
        assertTrue(repository.current.first().isActive)
    }

    @Test
    fun `finishing stops the rest timer`() = runTest {
        val repository = FakeSessionRepository(
            listOf(session(10, finished = false, exercises = arrayOf(sessionExercise(100, 1, completedSet(1, 10_000, 10))))),
        )
        val timer = timer()
        timer.start(60.seconds)

        FinishWorkoutSessionUseCase(repository, timer, fixedClock)(sessionId = 10)

        assertEquals(RestTimerState.Idle, timer.state.value)
    }

    @Test
    fun `discarding removes the active session`() = runTest {
        val repository = FakeSessionRepository(listOf(session(10, finished = false), session(20)))

        assertEquals(SessionResult.Success(Unit), DiscardWorkoutSessionUseCase(repository, timer())(10))
        assertEquals(listOf(20L), repository.current.map { it.id })
    }

    @Test
    fun `finished sessions cannot be discarded`() = runTest {
        val repository = FakeSessionRepository(listOf(session(20)))

        assertEquals(SessionResult.Failure(SessionError.NotFound), DiscardWorkoutSessionUseCase(repository, timer())(20))
        assertEquals(1, repository.current.size)
    }
}
