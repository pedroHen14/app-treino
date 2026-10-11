package com.pedro.treinoapp.feature.session.domain.usecase

import com.pedro.treinoapp.feature.session.domain.FakeSessionRepository
import com.pedro.treinoapp.feature.session.domain.TODAY
import com.pedro.treinoapp.feature.session.domain.completedSet
import com.pedro.treinoapp.feature.session.domain.model.SessionError
import com.pedro.treinoapp.feature.session.domain.model.SessionResult
import com.pedro.treinoapp.feature.session.domain.model.WorkoutSet
import com.pedro.treinoapp.feature.session.domain.session
import com.pedro.treinoapp.feature.session.domain.sessionExercise
import com.pedro.treinoapp.feature.workout.domain.model.RepRange
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SessionExerciseUseCasesTest {

    private val repository = FakeSessionRepository(
        listOf(
            session(
                10,
                finished = false,
                exercises = arrayOf(
                    sessionExercise(100, 1, WorkoutSet(id = 1), WorkoutSet(id = 2)).copy(targetReps = RepRange(8, 12)),
                    sessionExercise(200, 2, WorkoutSet(id = 3)),
                ),
            ),
            session(
                20,
                TODAY.minusDays(3),
                exercises = arrayOf(
                    sessionExercise(300, 3, completedSet(4, 30_000, 12), completedSet(5, 32_500, 10)),
                ),
            ),
        ),
    )
    private val prefill = BuildPrefilledSetsUseCase(repository)

    private val active get() = repository.current.first { it.id == 10L }

    @Test
    fun `exercises are added at the end, skipping repeated ones`() = runTest {
        val result = AddSessionExercisesUseCase(repository, prefill)(sessionId = 10, exerciseIds = listOf(3, 2, 4, 3))

        assertEquals(SessionResult.Success(Unit), result)
        assertEquals(listOf(1L, 2L, 3L, 4L), active.exercises.map { it.exerciseId })
    }

    @Test
    fun `added exercise repeats its last execution or gets three empty sets`() = runTest {
        AddSessionExercisesUseCase(repository, prefill)(10, listOf(3, 4))

        val (withHistory, withoutHistory) = active.exercises.takeLast(2)
        assertEquals(
            listOf(30_000 to 12, 32_500 to 10),
            withHistory.sets.map { it.weightGrams to it.reps },
        )
        assertEquals(3, withoutHistory.sets.size)
        assertNull(withoutHistory.targetReps)
    }

    @Test
    fun `exercise can be removed`() = runTest {
        assertEquals(SessionResult.Success(Unit), RemoveSessionExerciseUseCase(repository)(10, 100))

        assertEquals(listOf(2L), active.exercises.map { it.exerciseId })
    }

    @Test
    fun `replaced exercise keeps position, target and number of sets`() = runTest {
        val result = ReplaceSessionExerciseUseCase(repository, prefill)(10, sessionExerciseId = 100, exerciseId = 3)

        assertEquals(SessionResult.Success(Unit), result)
        val replaced = active.exercises.first()
        assertEquals(100L, replaced.id)
        assertEquals(3L, replaced.exerciseId)
        assertEquals(RepRange(8, 12), replaced.targetReps)
        assertEquals(listOf(30_000 to 12, 32_500 to 10), replaced.sets.map { it.weightGrams to it.reps })
    }

    @Test
    fun `cannot replace with an exercise already in the session`() = runTest {
        assertEquals(
            SessionResult.Failure(SessionError.DuplicateExercise),
            ReplaceSessionExerciseUseCase(repository, prefill)(10, sessionExerciseId = 100, exerciseId = 2),
        )
    }

    @Test
    fun `finished session exercises cannot be changed`() = runTest {
        assertEquals(
            SessionResult.Failure(SessionError.NotFound),
            AddSessionExercisesUseCase(repository, prefill)(20, listOf(1)),
        )
        assertEquals(SessionResult.Failure(SessionError.NotFound), RemoveSessionExerciseUseCase(repository)(20, 300))
    }
}
