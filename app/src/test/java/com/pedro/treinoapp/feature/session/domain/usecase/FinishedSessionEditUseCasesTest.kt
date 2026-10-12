package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.FakeSessionRepository
import com.pedro.treinoapp.feature.session.domain.TODAY
import com.pedro.treinoapp.feature.session.domain.completedSet
import com.pedro.treinoapp.feature.session.domain.fixedClock
import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSet
import com.pedro.treinoapp.feature.session.domain.session
import com.pedro.treinoapp.feature.session.domain.sessionExercise
import com.pedro.treinoapp.feature.session.domain.timer.RestTimer
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinishedSessionEditUseCasesTest {

    private val day = TODAY.minusDays(2)

    private val repository = FakeSessionRepository(
        listOf(
            session(10, finished = false, exercises = arrayOf(sessionExercise(100, 1, WorkoutSet(id = 1)))),
            session(
                20,
                day,
                exercises = arrayOf(
                    sessionExercise(
                        200,
                        1,
                        completedSet(2, 60_000, 10),
                        WorkoutSet(id = 3, weightGrams = 60_000, reps = 9),
                        WorkoutSet(id = 4),
                    ),
                    sessionExercise(300, 2, WorkoutSet(id = 5, weightGrams = 20_000)),
                ),
            ),
        ),
    )

    private fun finished() = repository.current.first { it.id == 20L }

    @Test
    fun `saving the edit completes filled sets at the session end and drops the empty ones`() = runTest {
        val result = SaveSessionEditUseCase(repository)(sessionId = 20)

        assertEquals(SessionResult.Success(Unit), result)
        val saved = finished()
        assertEquals(listOf(1L), saved.exercises.map { it.exerciseId })
        val sets = saved.exercises.single().sets
        assertEquals(listOf(2L, 3L), sets.map { it.id })
        assertTrue(sets.all { it.isCompleted })
        assertEquals(saved.finishedAt, sets.last().completedAt)
    }

    @Test
    fun `an edit that leaves no sets cannot be saved`() = runTest {
        RemoveSessionExerciseUseCase(repository)(20, 200)

        assertEquals(SessionResult.Failure(SessionError.NoCompletedSets), SaveSessionEditUseCase(repository)(20))
    }

    @Test
    fun `saving an edit is only for finished sessions`() = runTest {
        assertEquals(SessionResult.Failure(SessionError.NotFound), SaveSessionEditUseCase(repository)(10))
        assertEquals(SessionResult.Failure(SessionError.NotFound), SaveSessionEditUseCase(repository)(99))
    }

    @Test
    fun `a finished session cannot be finished or discarded again`() = runTest {
        val timer = RestTimer(backgroundScope, testScheduler.timeSource)

        assertEquals(SessionResult.Failure(SessionError.NotFound), FinishWorkoutSessionUseCase(repository, timer, fixedClock)(20))
        assertEquals(SessionResult.Failure(SessionError.NotFound), DiscardWorkoutSessionUseCase(repository, timer)(20))
    }

    @Test
    fun `deleting a finished session removes it from history`() = runTest {
        assertEquals(SessionResult.Success(Unit), DeleteFinishedSessionUseCase(repository)(20))

        assertNull(repository.current.find { it.id == 20L })
    }

    @Test
    fun `the session in progress cannot be deleted from history`() = runTest {
        assertEquals(SessionResult.Failure(SessionError.NotFound), DeleteFinishedSessionUseCase(repository)(10))
        assertEquals(SessionResult.Failure(SessionError.NotFound), DeleteFinishedSessionUseCase(repository)(99))
        assertEquals(2, repository.current.size)
    }
}
