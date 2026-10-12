package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.FakeRestTimerSettingsRepository
import com.pedro.treinoapp.feature.session.domain.FakeSessionRepository
import com.pedro.treinoapp.feature.session.domain.NOW
import com.pedro.treinoapp.feature.session.domain.TODAY
import com.pedro.treinoapp.feature.session.domain.completedSet
import com.pedro.treinoapp.feature.session.domain.fixedClock
import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSet
import com.pedro.treinoapp.feature.session.domain.session
import com.pedro.treinoapp.feature.session.domain.sessionExercise
import com.pedro.treinoapp.feature.session.domain.timer.RestTimer
import com.pedro.treinoapp.feature.session.domain.timer.RestTimerState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalCoroutinesApi::class)
class SetUseCasesTest {

    private val emptySet = WorkoutSet(id = 1)
    private val filledSet = WorkoutSet(id = 2, weightGrams = 60_000, reps = 10)
    private val doneSet = completedSet(3, 62_500, 8)

    private val repository = FakeSessionRepository(
        listOf(
            session(10, finished = false, exercises = arrayOf(sessionExercise(100, 1, emptySet, filledSet, doneSet))),
            session(20, TODAY.minusDays(2), exercises = arrayOf(sessionExercise(200, 1, completedSet(4, 50_000, 10)))),
        ),
    )
    private val settings = FakeRestTimerSettingsRepository()

    private fun set(id: Long) = repository.current.flatMap { it.exercises }.flatMap { it.sets }.first { it.id == id }

    private fun TestScope.completeSet(timer: RestTimer = RestTimer(backgroundScope, testScheduler.timeSource)) =
        CompleteSetUseCase(repository, settings, timer, fixedClock)

    @Test
    fun `weight and reps can be adjusted`() = runTest {
        val result = UpdateSetUseCase(repository)(sessionId = 10, setId = 1, weightGrams = 42_500, reps = 12)

        assertEquals(SessionResult.Success(Unit), result)
        assertEquals(42_500, set(1).weightGrams)
        assertEquals(12, set(1).reps)
    }

    @Test
    fun `weight must be a non negative multiple of half a kilo`() = runTest {
        val update = UpdateSetUseCase(repository)
        val invalid = SessionResult.Failure(SessionError.InvalidWeight)

        assertEquals(invalid, update(10, 1, weightGrams = 42_250, reps = 10))
        assertEquals(invalid, update(10, 1, weightGrams = -500, reps = 10))
        assertEquals(SessionResult.Success(Unit), update(10, 1, weightGrams = 0, reps = 10))
        assertEquals(SessionResult.Success(Unit), update(10, 1, weightGrams = 500, reps = 10))
    }

    @Test
    fun `reps must be at least one`() = runTest {
        assertEquals(
            SessionResult.Failure(SessionError.InvalidReps),
            UpdateSetUseCase(repository)(10, 1, weightGrams = 20_000, reps = 0),
        )
    }

    @Test
    fun `completed set cannot lose its values`() = runTest {
        assertEquals(
            SessionResult.Failure(SessionError.MissingValues),
            UpdateSetUseCase(repository)(10, 3, weightGrams = null, reps = 8),
        )
    }

    @Test
    fun `sets of a finished session can be edited`() = runTest {
        assertEquals(SessionResult.Success(Unit), UpdateSetUseCase(repository)(20, 4, weightGrams = 55_000, reps = 10))

        assertEquals(55_000, set(4).weightGrams)
        assertTrue(set(4).isCompleted)
    }

    @Test
    fun `sets of a finished session cannot be completed or uncompleted`() = runTest {
        assertEquals(SessionResult.Failure(SessionError.NotFound), completeSet()(sessionId = 20, setId = 4))
        assertEquals(SessionResult.Failure(SessionError.NotFound), UncompleteSetUseCase(repository)(20, 4))
        assertTrue(set(4).isCompleted)
    }

    @Test
    fun `completing a set marks it and starts the rest timer with the default duration`() = runTest {
        val timer = RestTimer(backgroundScope, testScheduler.timeSource)
        settings.setDefaultDuration(2.minutes)

        assertEquals(SessionResult.Success(Unit), completeSet(timer)(sessionId = 10, setId = 2))
        runCurrent()

        assertTrue(set(2).isCompleted)
        assertEquals(NOW, set(2).completedAt)
        assertEquals(RestTimerState.Running(2.minutes, 2.minutes), timer.state.value)
    }

    @Test
    fun `set without weight and reps cannot be completed`() = runTest {
        val timer = RestTimer(backgroundScope, testScheduler.timeSource)

        assertEquals(SessionResult.Failure(SessionError.MissingValues), completeSet(timer)(10, 1))
        assertEquals(RestTimerState.Idle, timer.state.value)
    }

    @Test
    fun `completion can be undone`() = runTest {
        assertEquals(SessionResult.Success(Unit), UncompleteSetUseCase(repository)(10, 3))

        assertEquals(false, set(3).isCompleted)
        assertEquals(null, set(3).completedAt)
    }

    @Test
    fun `added set copies the last set and is not completed`() = runTest {
        val id = (AddSetUseCase(repository)(sessionId = 10, sessionExerciseId = 100) as SessionResult.Success).value

        assertEquals(WorkoutSet(id = id, weightGrams = 62_500, reps = 8), set(id))
        assertEquals(4, repository.current.first().exercises.first().sets.size)
    }

    @Test
    fun `exercise has at most 20 sets`() = runTest {
        val add = AddSetUseCase(repository)
        repeat(17) { add(10, 100) }

        assertEquals(SessionResult.Failure(SessionError.TooManySets), add(10, 100))
    }

    @Test
    fun `set can be removed`() = runTest {
        assertEquals(SessionResult.Success(Unit), RemoveSetUseCase(repository)(10, 1))

        assertEquals(listOf(2L, 3L), repository.current.first().exercises.first().sets.map { it.id })
    }
}
